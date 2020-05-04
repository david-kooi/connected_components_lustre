import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import jkind.analysis.StaticAnalyzer;
import jkind.lustre.Node;
import jkind.lustre.Program;
import jkind.translation.Specification;
import jkind.translation.Translate;
import jkind.util.Util;
import jkind.StdErr;
import jkind.ExitCodes;
import jkind.lustre.VarDecl;
import jkind.lustre.ArrayType;
import jkind.lustre.RecordType;

import jkind.lustre.parsing.LustreLexer;
import jkind.lustre.parsing.LustreParser;
import jkind.lustre.parsing.StdErrErrorListener;
import jkind.lustre.parsing.LustreParseException; 
import jkind.lustre.parsing.LustreToAstVisitor;
import jkind.lustre.parsing.ValidIdChecker;
import jkind.lustre.parsing.FlattenIds;
import jkind.lustre.parsing.LustreParser.ProgramContext;

import jkind.slicing.DependencySet;
import jkind.slicing.Dependency;

import java.util.HashSet;

import com.google.common.collect.Sets;

import org.antlr.v4.runtime.ANTLRFileStream;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CommonTokenStream;



public class Main{

    public static void main(String[] args){
        try{
            String filename = args[0]; 
            Program program = parseLustre(filename); 
            program = Translate.translate(program);
            Specification spec = new Specification(program, true);            

            System.out.println("All properties");
            System.out.println(spec.node.properties);
            System.out.println("All inputs");
            System.out.println(spec.node.inputs);

            
            //System.out.println(spec.node.realizabilityInputs);
            //System.out.println(spec.dependencyMap.get("FSM001v3"));
           
           
            DependencySet d_shared = new DependencySet();
            
            ConnectedComponentSet CC_set = ComputeOutputConnectedComponents(spec); 

           CC_set.print(); 


        } catch (Throwable t){
            t.printStackTrace();
            System.exit(ExitCodes.UNCAUGHT_EXCEPTION);
        }
    }

    public static ConnectedComponentSet ComputeOutputConnectedComponents(Specification spec){
        /* Some pre-processing to isolate input/outputs */ 
        HashSet<Dependency> all_inputs = new HashSet<Dependency>();
        HashSet<Dependency> realizability_inputs = new HashSet<Dependency>();

        for (String name : spec.node.realizabilityInputs){
            realizability_inputs.add(Dependency.variable(name));
        } 
        for(VarDecl v : spec.node.inputs){
            all_inputs.add(Dependency.variable(v.id));
        }

        HashSet<Dependency> all_outputs = new HashSet<Dependency>(); 
        Sets.difference(all_inputs,realizability_inputs).copyInto(all_outputs); 

        System.out.println(all_outputs);

        /* End pre-processing */

        /* Start of algorithm from paper*/
        HashSet<ConnectedComponent> CC_set = new HashSet<>();
        boolean hasIntersection = false;

         // Y_i are the outputs of property P_i 
        HashSet<Dependency> Y_i = new HashSet<Dependency>();
        // P_i is the name of the property
        for(String P_i : spec.node.properties){ 
            // Set the outputs of P_i
            Y_i.clear();
            Sets.intersection(all_outputs, spec.dependencyMap.get(P_i).getSet()).copyInto(Y_i); 

            if(CC_set.isEmpty()){
                ConnectedComponent C0 = new ConnectedComponent(P_i, Y_i);
                CC_set.add(C0);
            }else{
                hasIntersection = false;
                for(ConnectedComponent C_j : CC_set){
                    if(Sets.intersection(C_j.outputs, Y_i).isEmpty() == false){
                        C_j.properties.add(P_i);
                        C_j.outputs.addAll(Y_i);                        
                        hasIntersection = true;
                        break;
                    } 
                }
                if(hasIntersection == false){
                    ConnectedComponent C_new = new ConnectedComponent(P_i, Y_i);
                    CC_set.add(C_new);
                }

            }
        }

        return new ConnectedComponentSet(CC_set);

    }

    public static Program parseLustre(String filename) throws Exception {
        File file = new File(filename);
                if (!file.exists() || !file.isFile()) {
                    StdErr.fatal(ExitCodes.FILE_NOT_FOUND, "cannot find file " + filename);
                }
                if (!file.canRead()) {
                    StdErr.fatal(ExitCodes.FILE_NOT_READABLE, "cannot read file " + filename);
                }

                StdErr.setLocationReference(readAllLines(filename));
                return parseLustre(new ANTLRFileStream(filename));
    }

    private static List<String> readAllLines(String filename) {
		Path path = FileSystems.getDefault().getPath(filename);
		try {
			return Files.readAllLines(path);
		} catch (IOException e) {
			return Collections.emptyList();
		}
	}

	public static Program parseLustre(CharStream stream) throws Exception {
		LustreLexer lexer = new LustreLexer(stream);
		CommonTokenStream tokens = new CommonTokenStream(lexer);
		LustreParser parser = new LustreParser(tokens);
		parser.removeErrorListeners();
		parser.addErrorListener(new StdErrErrorListener());
		ProgramContext program = parser.program();

		if (parser.getNumberOfSyntaxErrors() > 0) {
			System.exit(ExitCodes.PARSE_ERROR);
		}

		try {
			return flattenOrCheck(new LustreToAstVisitor().program(program));
		} catch (LustreParseException e) {
			StdErr.fatal(ExitCodes.PARSE_ERROR, e.getLocation(), e.getMessage());
			throw e;
		}
	}

	/**
	 * We allow extended ids (with ~ [ ] .) only when the program is a single
	 * node with simple types. This is useful for working with output from
	 * JLustre2Kind.
	 */
	private static Program flattenOrCheck(Program program) {
		if (isSimple(program)) {
			return new FlattenIds().visit(program);
		} else {
			if (!ValidIdChecker.check(program)) {
				System.exit(ExitCodes.PARSE_ERROR);
			}
			return program;
		}
	}

	private static boolean isSimple(Program program) {
		if (!program.types.isEmpty()) {
			return false;
		} else if (!program.constants.isEmpty()) {
			return false;
		} else if (program.nodes.size() != 1) {
			return false;
		}

		Node main = program.getMainNode();
		for (VarDecl vd : Util.getVarDecls(main)) {
			if (vd.type instanceof ArrayType || vd.type instanceof RecordType) {
				return false;
			}
		}
		return true;
	}



}


