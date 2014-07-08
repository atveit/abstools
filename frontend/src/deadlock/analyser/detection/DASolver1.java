package deadlock.analyser.detection;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import deadlock.analyser.factory.Contract;
import deadlock.analyser.factory.ContractElement;
import deadlock.analyser.factory.ContractElementAwait;
import deadlock.analyser.factory.ContractElementGet;
import deadlock.analyser.factory.ContractElementInvk;
import deadlock.analyser.factory.ContractElementInvkA;
import deadlock.analyser.factory.ContractElementInvkG;
import deadlock.analyser.factory.ContractElementSyncInvk;
import deadlock.analyser.factory.ContractElementUnion;
import deadlock.analyser.factory.Factory;
import deadlock.analyser.factory.GroupName;
import deadlock.analyser.factory.MethodContract;
import deadlock.analyser.factory.MethodInterface;
import deadlock.analyser.factory.Record;
import deadlock.constraints.term.Term;
import deadlock.constraints.term.TermStructured;
//import deadlock.constraints.term.Variable;
//import deadlock.constraints.term.TermVariable;


public class DASolver1 extends DASolver {


    Boolean saturation;
   
    
    int nOfIterations;

    

    int nOfDep;

    Boolean livelock;

    public DASolver1(Factory f, Map<String, Term> map, int i){
        super(f, map);
        
        this.nOfIterations = i;
        this.nOfDep = 0;

        this.saturation = false;
        this.livelock = false;        
    }

    //this method performs the deadlock analysis using a fix point algorithm 
    @Override
    public void computeSolution(){
        
        //perform an infinite cycle, since thanks to the saturation a fix point is always reached
        for(int i=0; ; i++){
            
            //perform one step expansion for each method
            for(String mName : methodMap.keySet()){

                //get the method contract
                Term contr = methodMap.get(mName);

                // I want to isolate the contract (body contract), only Main.main has already the right contract
                if(contr instanceof MethodContract){
                    contr = ((MethodContract) contr).getContract();
                }

                // In this first version of algorithm I want to work only with contract single (not contractSeq)
                // but inference return always a contractSeq, even if it is a single contract, so, I 'clear' it, if 
                // I have a contractSeq with only one subterm inside, it will be our contract to check
                if(((Contract) contr).getList().size() == 1){
                    contr = ((Contract) contr).getList().get(0);
                }

                //get the appropriate var substitution which is the last applied if there is saturation
                //otherwise create a new one
                VarSubstitution subFresh;
                if(this.saturation){
                     subFresh = lampMap.get(mName).getLastBFresh();
                }
                    
                else{
                    Set<GroupName> bTilde = lampMap.get(mName).getbTilde();
                    subFresh = new VarSubstitution();
                    for(GroupName v : bTilde) subFresh.addSub( v, df.newGroupName());
                }

                //resulting lam after expansion
                DoubleLam expansion;
                
                //apply the corresponding rule
                if(contr instanceof ContractElementGet)             { expansion = wGet(mName, (ContractElementGet) contr, subFresh);}
                else if(contr instanceof ContractElementAwait)      { expansion = wAwait(mName, (ContractElementAwait) contr, subFresh);}
                else if(contr instanceof ContractElementInvk)       { expansion = wInvk(mName, (ContractElementInvk) contr, subFresh);}
                else if(contr instanceof ContractElementSyncInvk)   { expansion = wSyncInvk(mName, (ContractElementSyncInvk) contr, subFresh);}
                else if(contr instanceof ContractElementInvkG)      { expansion = wGInvk(mName, (ContractElementInvkG) contr, subFresh);}
                else if(contr instanceof ContractElementInvkA)      { expansion = wAInvk(mName, (ContractElementInvkA) contr, subFresh);}
                else if(contr instanceof ContractElementUnion)      { expansion = wUnion(mName, (ContractElementUnion) contr, subFresh);}
                else                                                { expansion = wSeq(mName, (Contract) contr, subFresh);}
                
                //Update the lam with the expansion result
                BigLam lam = lampMap.get(mName);
                
                lam.setFirst(expansion.getW());
                lam.setSecond(expansion.getWPrime());
                lam.setLastBFresh(subFresh);
            }

            //check for cycles            
            BigLam blMain = lampMap.get("Main.main");
            if(blMain.hasCycle()){
                //a cyclic dependency was found
                //if there is a pure await dependencies cycle then there is livelock
                this.deadlock = blMain.hasCycleGet();
                
                //if the cycle found is no a livelock then this lock is indeed a deadlock
                this.livelock = !this.deadlock;
                
                //is it possible for a program to have more than one potential deadlocks and 
                //livelocks at the same time but the algorithm stop at the first cycle so
                //only one cyclic dependency is reported
                return;
            }

            //if the number of dependencies has not change in this iteration then a fix point is found
            //check if the number of dependencies is different of previous, if not, stop the analysis
            int newNumberOfDep = 0;
            for(String mName : lampMap.keySet()){
                newNumberOfDep += lampMap.get(mName).getFirst().numberOfDep();
                newNumberOfDep += lampMap.get(mName).getSecond().numberOfDep();
            }
            if(newNumberOfDep == this.nOfDep) return;
            else this.nOfDep = newNumberOfDep;
            
            //if the default number of iterations is reached then saturate
            this.saturation = i >= nOfIterations;
        }
    }

    // The rule W-Gzero of the Analysis
    private DoubleLam wGet(String mName, ContractElementGet cGet, VarSubstitution bFresh){
        // it will contains the result of the application of the rule
        DoubleLam l = new DoubleLam();                
        // here we extract the 2 variable from the contractGet and apply on them the fresh renaming (bTilde)
        GroupName a = cGet.whosWaiting();
        GroupName b = cGet.whosWaited();
        a = bFresh.apply(a);
        b = bFresh.apply(b);            
        // here we calculate the solution of the application of the rule
        Lam w = new Lam();
        Lam wPrime = new Lam();
        w.addCouple(a, b);
       
        //I learn reading again the paper that only the first lamp obtain the couple
        //wPrime.addCouple(a, b);               
        // we compose and return the solution <w,wPrime>
        l.setW(w);
        l.setWPrime(wPrime);
        return l;       
    }

    // The rule W-Azero of the Analysis
    private DoubleLam wAwait(String mName, ContractElementAwait cAwait, VarSubstitution bFresh){
        // it will contains the result of the application of the rule
        DoubleLam l = new DoubleLam();

        // here we extract the 2 variable from the contractGet and apply on them the fresh renaming (bTilde)

        /* THESE TWO ERRORS WILL BE FIXED WHEN WE PASS ON THE NEW DEFINITION OF CONTRACT get and await */
        GroupName a = cAwait.whosWaiting();
        GroupName b = cAwait.whosWaited();
        a = bFresh.apply(a);
        b = bFresh.apply(b);

        // here we calculate the solution of the application of the rule
        Lam w = new Lam();
        Lam wPrime = new Lam();
        w.addCoupleAwait(a, b);
        
        //I learn reading again the paper that only the first lamp obtain the couple
        //wPrime.addCouple(a, b);

        // we compose and return the solution <w,wPrime>
        l.setW(w);
        l.setWPrime(wPrime);
        return l;       
    }

    // The rule W-Invk of the Analysis
    private DoubleLam wInvk(String mName, ContractElementInvk cInvk, VarSubstitution bFresh){
        // it will contains the result of the application of the rule
        DoubleLam l = new DoubleLam();

        // here I recover and clear the method called, after that I have a string that identify it
        String method = cInvk.getClassName() + "." + cInvk.getMethodName();
        //System.out.println("method called is " + method.toString());

        //here we recover the bigLamp of the method called and also is methodContract
        BigLam bLamp = lampMap.get(method);
        MethodContract methodContract = (MethodContract) methodMap.get(method);
        //if(bLamp != null) System.out.println("BigLamp of method called is: " + bLamp.toString());
        //if(methodContract != null) System.out.println("methodContract of method called is: " + methodContract.toString());
        //System.out.println("fresh renaming is: " + bFresh.toString());

        //The two Lamp of methodInvokation rule
        Lam w = new Lam();
        Lam wfirstPrime = new Lam(); //this is done to avoid side effect
        wfirstPrime.addLamp(bLamp.getFirst());
        Lam wsecondPrime = new Lam();
        wsecondPrime.addLamp(bLamp.getSecond());

        Lam wPrime = new Lam();
        wPrime.addLamp(wfirstPrime);
        wPrime.addLamp(wsecondPrime);

        if(!this.saturation){
            //here we recover the formal parameter of the method invoked
            Set<GroupName> aTilde = bLamp.getaTilde();
            //System.out.println("aTilde of method called is: " + aTilde);

            //here we create the fresh substitution for NON formal parameter
            VarSubstitution subParam = new VarSubstitution();

            //here we recover ALL the free variable of the lamp of the method invoked and remove the variable of the formal parameters
            Set<GroupName> aPrimeTilde = bLamp.getFirst().fv();
            aPrimeTilde.addAll(bLamp.getSecond().fv());
            Set<GroupName> aTildeTermVar = new TreeSet<GroupName>();
            for(GroupName v : aTilde) aTildeTermVar.add(v);
            aPrimeTilde.removeAll(aTildeTermVar);
            //System.out.println("aPrimeTilde to substitute is :" + aPrimeTilde.toString() );

            for(GroupName v : aPrimeTilde) subParam.addSub(v, df.newGroupName());

            //I apply the first substitution, the once for formal parameter
            wPrime.apply(subParam);
        }



        //here we create and apply the second substitution, 'thisRecord' of method called got to be replaced with
        //'thisRecord' of the call inside the contract that we are analyzing
        VarSubstitution subThis;
        MethodInterface interfaceCaller = cInvk.getMethodInterface();
        Record thisCaller = interfaceCaller.getThis();
        MethodInterface interfaceCalled = methodContract.getMethodInterface();
        Record thisCalled = interfaceCalled.getThis();
        //System.out.println("thisCaller = " + thisCaller.toString());
        //System.out.println("thisCalled = " + thisCalled.toString());
        subThis = findSub(thisCaller, thisCalled, bFresh);
        //System.out.println("subThis = " + subThis.toString());
        wPrime.apply(subThis);

        //here we create and apply the third substitution, 'argsRecord' of method called got to be replaced with
        //'argsRecord' of the call inside the contract that we are analyzing
        VarSubstitution subArgs;

        List<Record> argsCaller = interfaceCaller.getParameters();

        List<Record> argsCalled = interfaceCalled.getParameters();

        for(int i = 0 ; i<argsCaller.size() ; i++){
            subArgs = findSub(argsCaller.get(i), argsCalled.get(i), bFresh);
            wPrime.apply(subArgs);
        }

        //here we create and apply the third substitution, 'retRecord' of method called got to be replaced with
        //'retRecord' of the call inside the contract that we are analyzing
        VarSubstitution subRet;
        Record retCaller =  interfaceCaller.getResult();
        Record retCalled =  interfaceCalled.getResult();
        //System.out.println("retCaller = " + retCaller.toString());
        //System.out.println("retCalled = " + retCalled.toString());
        subRet = findSub(retCaller, retCalled, bFresh);
        //System.out.println("subRet = " + subRet.toString());
        wPrime.apply(subRet);

        // we compose and return the solution <w,wPrime>
        l.setW(w);
        l.setWPrime(wPrime);
        return l;       
    }
    
    // The rule W-Invk of the Analysis
    private DoubleLam wSyncInvk(String mName, ContractElementSyncInvk cInvk, VarSubstitution bFresh){
        // it will contains the result of the application of the rule
        DoubleLam l = new DoubleLam();

        // here I recover and clear the method called, after that I have a string that identify it
        String method = cInvk.getClassName() + "." + cInvk.getMethodName();
        //System.out.println("method called is " + method.toString());

        //here we recover the bigLamp of the method called and also is methodContract
        BigLam bLamp = lampMap.get(method);
        MethodContract methodContract = (MethodContract) methodMap.get(method);
        //if(bLamp != null) System.out.println("BigLamp of method called is: " + bLamp.toString());
        //if(methodContract != null) System.out.println("methodContract of method called is: " + methodContract.toString());
        //System.out.println("fresh renaming is: " + bFresh.toString());

        //The two Lamp of methodInvokation rule
        Lam w = new Lam();
        Lam wPrime = new Lam(); //this is done to avoid side effect
        w.addLamp(bLamp.getFirst());
        wPrime.addLamp(bLamp.getSecond());


        if(!this.saturation){
            //here we recover the formal parameter of the method invoked
            Set<GroupName> aTilde = bLamp.getaTilde();
            //System.out.println("aTilde of method called is: " + aTilde);

            //here we create the fresh substitution for NON formal parameter
            VarSubstitution subParam = new VarSubstitution();

            //here we recover ALL the free variable of the lamp of the method invoked and remove the variable of the formal parameters
            Set<GroupName> aPrimeTilde = bLamp.getFirst().fv();
            aPrimeTilde.addAll(bLamp.getSecond().fv());
            Set<GroupName> aTildeTermVar = new TreeSet<GroupName>();
            for(GroupName v : aTilde) aTildeTermVar.add(v);
            aPrimeTilde.removeAll(aTildeTermVar);
            //System.out.println("aPrimeTilde to substitute is :" + aPrimeTilde.toString() );

            for(GroupName v : aPrimeTilde) subParam.addSub(v, df.newGroupName());

            //I apply the first substitution, the once for formal parameter
            w.apply(subParam);
            wPrime.apply(subParam);
        }



        //here we create and apply the second substitution, 'thisRecord' of method called got to be replaced with
        //'thisRecord' of the call inside the contract that we are analyzing
        VarSubstitution subThis;
        MethodInterface interfaceCaller = cInvk.getMethodInterface();
        Record thisCaller = interfaceCaller.getThis();
        MethodInterface interfaceCalled = methodContract.getMethodInterface();
        Record thisCalled = interfaceCalled.getThis();
        //System.out.println("thisCaller = " + thisCaller.toString());
        //System.out.println("thisCalled = " + thisCalled.toString());
        subThis = findSub(thisCaller, thisCalled, bFresh);
        //System.out.println("subThis = " + subThis.toString());
        w.apply(subThis);
        wPrime.apply(subThis);

        //here we create and apply the third substitution, 'argsRecord' of method called got to be replaced with
        //'argsRecord' of the call inside the contract that we are analyzing
        VarSubstitution subArgs;

        List<Record> argsCaller = interfaceCaller.getParameters();

        List<Record> argsCalled = interfaceCalled.getParameters();

        for(int i = 0 ; i<argsCaller.size() ; i++){
            subArgs = findSub(argsCaller.get(i), argsCalled.get(i), bFresh);
            w.apply(subArgs);
            wPrime.apply(subArgs);
        }

        //here we create and apply the third substitution, 'retRecord' of method called got to be replaced with
        //'retRecord' of the call inside the contract that we are analyzing
        VarSubstitution subRet;
        Record retCaller =  interfaceCaller.getResult();
        Record retCalled =  interfaceCalled.getResult();
        //System.out.println("retCaller = " + retCaller.toString());
        //System.out.println("retCalled = " + retCalled.toString());
        subRet = findSub(retCaller, retCalled, bFresh);
        //System.out.println("subRet = " + subRet.toString());
        w.apply(subRet);
        wPrime.apply(subRet);

        // we compose and return the solution <w,wPrime>
        l.setW(w);
        l.setWPrime(wPrime);
        return l;       
    }
    
    // The rule W-GInvk of the Analysis
    private DoubleLam wGInvk(String mName, ContractElementInvkG cGInvk, VarSubstitution bFresh){
        // it will contains the result of the application of the rule
        DoubleLam l = new DoubleLam();

        // here I split cGInvk between the invocation cInvk and the get cGet
        ContractElementInvk cInvk = cGInvk.getInvk();
        ContractElementGet cGet = cGInvk.getGet();

        // here I recover and clear the method called, after that I have a string that identify it
        String method = cInvk.getClassName() +"."+ cInvk.getMethodName();
        //System.out.println("method called is " + method.toString());

        //here we recover the bigLamp of the method called and also is methodContract
        BigLam bLamp = lampMap.get(method);
        MethodContract methodContract = (MethodContract) methodMap.get(method);
        //if(bLamp != null) System.out.println("BigLamp of method called is: " + bLamp.toString());
        //if(methodContract != null) System.out.println("methodContract of method called is: " + methodContract.toString());
        //System.out.println("fresh renaming is: " + bFresh.toString());

        Lam w = new Lam(); //this is done to avoid side effect
        w.addLamp(bLamp.getFirst());
        Lam wPrime = new Lam();
        wPrime.addLamp(bLamp.getSecond());

        if(!this.saturation){
            //here we recover the formal parameter of the method invoked
            Set<GroupName> aTilde = bLamp.getaTilde();
            //System.out.println("aTilde of method called is: " + aTilde);


            //here we create the fresh substitution for NON formal parameter
            VarSubstitution subParam = new VarSubstitution();

            //here we recover ALL the free variable of the lamp of the method invoked and remove the variable of the formal parameters
            Set<GroupName> aPrimeTilde = bLamp.getFirst().fv();
            aPrimeTilde.addAll(bLamp.getSecond().fv());
            Set<GroupName> aTildeTermVar = new TreeSet<GroupName>();
            for(GroupName v : aTilde) aTildeTermVar.add(v);
            aPrimeTilde.removeAll(aTildeTermVar);

            //System.out.println("aPrimeTilde to substitute is :" + aPrimeTilde.toString() );

            for(GroupName v : aPrimeTilde) subParam.addSub(v, df.newGroupName());
            //The two Lamp of methodInvokation rule

            //I apply the first substitution, the once for formal parameter
            w.apply(subParam);
            wPrime.apply(subParam);
        }







        //here we create and apply the second substitution, 'thisRecord' of method called got to be replaced with
        //'thisRecord' of the call inside the contract that we are analyzing
        VarSubstitution subThis;
        MethodInterface interfaceCaller = cInvk.getMethodInterface();
        Record thisCaller = interfaceCaller.getThis();
        MethodInterface interfaceCalled = methodContract.getMethodInterface();
        Record thisCalled = interfaceCalled.getThis();
        //System.out.println("thisCaller = " + thisCaller.toString());
        //System.out.println("thisCalled = " + thisCalled.toString());
        subThis = findSub(thisCaller, thisCalled, bFresh);
        //System.out.println("subThis = " + subThis.toString());
        w.apply(subThis);
        wPrime.apply(subThis);


        //here we create and apply the third substitution, 'argsRecord' of method called got to be replaced with
        //'argsRecord' of the call inside the contract that we are analyzing
        VarSubstitution subArgs;
        List<Record> argsCaller = interfaceCaller.getParameters();

        List<Record> argsCalled = interfaceCalled.getParameters();

        for(int i = 0 ; i<argsCaller.size() ; i++){
            subArgs = findSub(argsCaller.get(i), argsCalled.get(i), bFresh);
            w.apply(subArgs);
            wPrime.apply(subArgs);
        }


        //here we create and apply the third substitution, 'retRecord' of method called got to be replaced with
        //'retRecord' of the call inside the contract that we are analyzing
        VarSubstitution subRet;
        Record retCaller =  interfaceCaller.getResult();
        Record retCalled =  interfaceCalled.getResult();
        //System.out.println("retCaller = " + retCaller.toString());
        //System.out.println("retCalled = " + retCalled.toString());
        subRet = findSub(retCaller, retCalled, bFresh);
        //System.out.println("subRet = " + subRet.toString());
        w.apply(subRet);
        wPrime.apply(subRet);


        // here we extract the 2 variable from the contractGet and apply on them the fresh renaming (bTilde)
        GroupName a = cGet.whosWaiting();
        GroupName b = cGet.whosWaited();
        //System.out.println("getVar are = " + a.toString() + " and " + b.toString());
        a = bFresh.apply(a);
        b = bFresh.apply(b);

        //System.out.println("getVar after Sub = " + a.toString() + " and " + b.toString());

        // we add the get Pair of names at the two lamps
        w.addCouple(a, b);
        
        //same comment of the rule w-Gzero
        //wPrime.addCouple(a, b);


        // we compose and return the solution <w,wPrime>
        l.setW(w);
        l.setWPrime(wPrime);
        return l;       
    }

    // The rule W-AInvk of the Analysis
    private DoubleLam wAInvk(String mName, ContractElementInvkA cAInvk, VarSubstitution bFresh){
        // it will contains the result of the application of the rule
        DoubleLam l = new DoubleLam();

        // here I split cGInvk between the invocation cInvk and the get cGet
        ContractElementInvk cInvk = cAInvk.getInvk();
        ContractElementAwait cAwait = cAInvk.getAwait();

        // here I recover and clear the method called, after that I have a string that identify it
        String method = cInvk.getClassName() +"."+ cInvk.getMethodName();
        //System.out.println("method called is " + method.toString());

        //here we recover the bigLamp of the method called and also is methodContract
        BigLam bLamp = lampMap.get(method);
        MethodContract methodContract = (MethodContract) methodMap.get(method);
        //if(bLamp != null) System.out.println("BigLamp of method called is: " + bLamp.toString());
        //if(methodContract != null) System.out.println("methodContract of method called is: " + methodContract.toString());
        //System.out.println("fresh renaming is: " + bFresh.toString());

        //The two Lamp of methodInvokation rule
        Lam w = new Lam(); //this is done to avoid side effect
        w.addLamp(bLamp.getFirst());
        Lam wPrime = new Lam();
        wPrime.addLamp(bLamp.getSecond());

        if(!this.saturation){
            //here we recover the formal parameter of the method invoked
            Set<GroupName> aTilde = bLamp.getaTilde();
            //System.out.println("aTilde of method called is: " + aTilde);

            //here we create the fresh substitution for NON formal parameter
            VarSubstitution subParam = new VarSubstitution();

            //here we recover ALL the free variable of the lamp of the method invoked and remove the variable of the formal parameters
            Set<GroupName> aPrimeTilde = bLamp.getFirst().fv();
            aPrimeTilde.addAll(bLamp.getSecond().fv());
            Set<GroupName> aTildeTermVar = new TreeSet<GroupName>();
            for(GroupName v : aTilde) aTildeTermVar.add(v);
            aPrimeTilde.removeAll(aTildeTermVar);

            //System.out.println("aPrimeTilde to substitute is :" + aPrimeTilde.toString() );

            for(GroupName v : aPrimeTilde) subParam.addSub(v, df.newGroupName());
            //I apply the first substitution, the once for formal parameter
            w.apply(subParam);
            wPrime.apply(subParam);
        }



        //here we create and apply the second substitution, 'thisRecord' of method called got to be replaced with
        //'thisRecord' of the call inside the contract that we are analyzing
        VarSubstitution subThis;
        MethodInterface interfaceCaller = cInvk.getMethodInterface();
        Record thisCaller = interfaceCaller.getThis();
        MethodInterface interfaceCalled = methodContract.getMethodInterface();
        Record thisCalled = interfaceCalled.getThis();
        //System.out.println("thisCaller = " + thisCaller.toString());
        //System.out.println("thisCalled = " + thisCalled.toString());
        subThis = findSub(thisCaller, thisCalled, bFresh);
        //System.out.println("subThis = " + subThis.toString());
        w.apply(subThis);
        wPrime.apply(subThis);

        //here we create and apply the third substitution, 'argsRecord' of method called got to be replaced with
        //'argsRecord' of the call inside the contract that we are analyzing
        VarSubstitution subArgs;
        List<Record> argsCaller = interfaceCaller.getParameters();

        List<Record> argsCalled = interfaceCalled.getParameters();

        for(int i = 0 ; i<argsCaller.size() ; i++){
            subArgs = findSub(argsCaller.get(i), argsCalled.get(i), bFresh);
            w.apply(subArgs);
            wPrime.apply(subArgs);
        }

        //here we create and apply the third substitution, 'retRecord' of method called got to be replaced with
        //'retRecord' of the call inside the contract that we are analyzing
        VarSubstitution subRet;
        Record retCaller =  interfaceCaller.getResult();
        Record retCalled =  interfaceCalled.getResult();
        //System.out.println("retCaller = " + retCaller.toString());
        //System.out.println("retCalled = " + retCalled.toString());
        subRet = findSub(retCaller, retCalled, bFresh);
        //System.out.println("subRet = " + subRet.toString());
        w.apply(subRet);
        wPrime.apply(subRet);

        // here we extract the 2 variable from the contractGet and apply on them the fresh renaming (bTilde)
        GroupName a = cAwait.whosWaiting();
        GroupName b = cAwait.whosWaited();
        //System.out.println("awaitVar are = " + a.toString() + " and " + b.toString());
        a = bFresh.apply(a);
        b = bFresh.apply(b);
        //System.out.println("awaitVar after Sub = " + a.toString() + " and " + b.toString());

        // we add the get Pair of names at the two lamps
        w.addCoupleAwait(a, b);
        
        //same comment of the rule w-Gzero
        //wPrime.addCouple(a, b);

        // we compose and return the solution <w,wPrime>
        l.setW(w);
        l.setWPrime(wPrime);
        return l;       
    }

    // The rule W-Union of the Analysis
    private DoubleLam wUnion(String mName, ContractElementUnion contr, VarSubstitution bFresh){

        Contract c1 = contr.getBranchOne();
        Contract c2 = contr.getBranchTwo();

        DoubleLam l1 = wSeq(mName, (Contract) c1, bFresh);
        DoubleLam l2 = wSeq(mName, (Contract) c2, bFresh);

        DoubleLam l = new DoubleLam();
        l.union(l1, l2);

        return l;       
    }       


    // The rule W-Seq of the Analysis
    public DoubleLam wSeq(String mName, Contract contr, VarSubstitution bFresh){
        List<ContractElement> contracts = ((Contract) contr).getList();
        DoubleLam l = new DoubleLam();
        
        for(ContractElement c : contracts){
            if(c instanceof ContractElementGet) {
                DoubleLam lr = wGet(mName, (ContractElementGet) c, bFresh);
                l.seqComposition(lr);   
            }else if(c instanceof ContractElementAwait) {
                DoubleLam lr = wAwait(mName, (ContractElementAwait) c, bFresh);
                l.seqComposition(lr);   
            }else if(c instanceof ContractElementSyncInvk){ //this means that contr is a ContractSyncInvk
                DoubleLam lr = wSyncInvk(mName, (ContractElementSyncInvk) c, bFresh);
                l.seqComposition(lr);   
            }else if(c instanceof ContractElementInvk){ //this means that contr is a ContractInvk
                DoubleLam lr = wInvk(mName, (ContractElementInvk) c, bFresh);
                l.seqComposition(lr);   
            }else if(c instanceof ContractElementInvkG){
                DoubleLam lr = wGInvk(mName, (ContractElementInvkG) c, bFresh);
                l.seqComposition(lr);   
            }else if(c instanceof ContractElementInvkA){
                DoubleLam lr = wAInvk(mName, (ContractElementInvkA) c, bFresh);
                l.seqComposition(lr);   
            }else if(c instanceof ContractElementUnion){
                DoubleLam lr = wUnion(mName, (ContractElementUnion) c, bFresh);
                l.seqComposition(lr);   
            }
        }
        return l;       
    }       

    // The rule W-SeqInUnion of the Analysis
//    private DoubleLam wSeqInUnion(String mName, Contract contr, VarSubstitution bFresh){
//        List<ContractElement> contracts = ((Contract) contr).getList();
//        DoubleLam l = new DoubleLam();
//        for(ContractElement c : contracts){
//            if(c instanceof ContractElementGet) {
//                DoubleLam lr = wGet(mName, (ContractElementGet) c, bFresh);
//                l.seqComposition(lr);   
//            }else if(c instanceof ContractElementAwait) {
//                DoubleLam lr = wAwait(mName, (ContractElementAwait) c, bFresh);
//                l.seqComposition(lr);   
//            }else if(c instanceof ContractElementInvk){ //this means that contr is a ContractInvk
//                DoubleLam lr = wInvk(mName, (ContractElementInvk) c, bFresh);
//                l.seqComposition(lr);   
//            }else if(c instanceof ContractElementInvkG){
//                DoubleLam lr = wGInvk(mName, (ContractElementInvkG) c, bFresh);
//                l.seqComposition(lr);   
//            }else if(c instanceof ContractElementInvkA){
//                DoubleLam lr = wAInvk(mName, (ContractElementInvkA) c, bFresh);
//                l.seqComposition(lr);   
//            }
//        }
//        return l;       
//    }       

    //final info like saturation and various lock

    public Boolean isSatured(){
        return this.saturation;
    }

   

    public boolean isLivelockMain(){
        return this.livelock;
    }

    public VarSubstitution findSub(Term t1, Term t2, VarSubstitution preSub){
        VarSubstitution sub = new VarSubstitution();
        if(t1 instanceof GroupName && t2 instanceof GroupName){
            GroupName v = preSub.apply((GroupName) t1);
            sub.addSub((GroupName) t2, v);
            return sub;
        }

        if(t1 instanceof TermStructured && t2 instanceof TermStructured){
            List<Term> lt1 = ((TermStructured) t1).getSubTerms();
            List<Term> lt2 = ((TermStructured) t2).getSubTerms();
            VarSubstitution subsub = new VarSubstitution();
            for(int i = 0 ; i<lt1.size() ; i++){
                subsub = findSub(lt1.get(i), lt2.get(i), preSub);
                sub.addSub(subsub);
            }
        }

        return sub;     
    }

    public String toString(){
        String res = "";
        for(String name : this.lampMap.keySet()){
            res += (name + " = \n" + lampMap.get(name).toString() + "\n");
        }
        return res;
    }

   

}
