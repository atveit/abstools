/**
 * Copyright (c) 2009-2011, The HATS Consortium. All rights reserved. 
 * This file is licensed under the terms of the Modified BSD License.
 */
package autodeploy;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;

import abs.common.WrongProgramArgumentException;
import abs.frontend.ast.Model;
import abs.frontend.delta.DeltaModellingException;
import abs.frontend.parser.Main;

public class Tester extends Main {
    
  private String _JSONName = "toto.json";

  public static void main(final String... args) {
    try { new Tester().compile(args); }
    catch (Exception e) {
      System.err.println("An error occurred during compilation: " + e.getMessage());
      if (Arrays.asList(args).contains("-debug")) { e.printStackTrace(); }
      System.exit(1);  
    }
  }

  private void compile(String[] args)
      throws DeltaModellingException, IOException, WrongProgramArgumentException, ParserConfigurationException,FileNotFoundException {
    final Model model = this.parse(args);
    if (model.hasParserErrors() || model.hasErrors() || model.hasTypeErrors()) return;
    if (verbose) { System.out.println("Starting Dependency information extraction..."); }
    DeployInformation di = new DeployInformation();
    di.extractInformation(model);
    if (verbose) { System.out.println("Starting JSON generation..."); }
    PrintWriter f = new PrintWriter(new File(_JSONName));
    di.generateJSON(f);
    f.close();
  }



  @Override
  public List<String> parseArgs(String[] args) {
    List<String> restArgs = super.parseArgs(args);
    List<String> remainingArgs = new ArrayList<String>();
    for (int i = 0; i < restArgs.size(); i++) {
      String arg = restArgs.get(i);
      if (arg.startsWith("-JSON=")){
        try{ _JSONName = arg.split("=")[1]; }
        catch (Exception e) {
          System.err.println("The number of iterations (-it) should be an integer");
          System.exit(1);
        }
      } else { remainingArgs.add(arg); }
    }
    return remainingArgs;
  }
    
  @Override
  protected void printUsage() {
    super.printUsage();
    System.out.println("Deadlock analyzer:\n  -JSON=<var>     name of the generated JSON file\n");
  }

}


