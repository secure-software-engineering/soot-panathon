package soot.javaToJimple;
import polyglot.main.*;
import polyglot.frontend.*;
import polyglot.util.*;
import polyglot.visit.*;
import polyglot.ast.*;

import java.util.*;
import java.io.*;

public class JavaToJimple {
	
    public static final polyglot.frontend.Pass.ID CAST_INSERTION = new polyglot.frontend.Pass.ID("cast-insertion");
    public static final polyglot.frontend.Pass.ID SAVE_AST = new polyglot.frontend.Pass.ID("save-ast");
    
    /**
     * sets up the info needed to invoke polyglot
     */
	public polyglot.frontend.ExtensionInfo initExtInfo(String fileName, List sourceLocations){
		
        Set source = new HashSet();
        ExtensionInfo extInfo = new soot.javaToJimple.jj.ExtensionInfo() {
            public List passes(Job job) {
                List passes = super.passes(job);
                beforePass(passes, Pass.TYPE_CHECK, new VisitorPass(polyglot.frontend.Pass.FOLD, job, new polyglot.visit.ConstantFolder(ts, nf)));
                beforePass(passes, Pass.EXIT_CHECK, new VisitorPass(CAST_INSERTION, job, new CastInsertionVisitor(job, ts, nf)));
                afterPass(passes, Pass.PRE_OUTPUT_ALL, new SaveASTVisitor(SAVE_AST, job, this));
                removePass(passes, Pass.OUTPUT);
                return passes;
            }
            
        };
        polyglot.main.Options options = extInfo.getOptions();

        options.assertions = true;
        options.source_path = new LinkedList();
        Iterator it = sourceLocations.iterator();
        while (it.hasNext()){
            Object next = it.next();
            options.source_path.add(new File(next.toString()));
        }

        options.source_ext = "java";
		options.serialize_type_info = false;
		
		source.add(fileName);
		
		options.source_path.add(new File(fileName).getParentFile());
		
        polyglot.main.Options.global = options;

        return extInfo;
    }
	
    /**
     * uses polyglot to compile source and build AST
     */
    public polyglot.ast.Node compile(polyglot.frontend.Compiler compiler, String fileName, polyglot.frontend.ExtensionInfo extInfo){
		SourceLoader source_loader = compiler.sourceExtension().sourceLoader();

		try {
			FileSource source = new FileSource(fileName);

            SourceJob job = null;

            if (compiler.sourceExtension() instanceof soot.javaToJimple.jj.ExtensionInfo){
                soot.javaToJimple.jj.ExtensionInfo jjInfo = (soot.javaToJimple.jj.ExtensionInfo)compiler.sourceExtension();
                if (jjInfo.sourceJobMap() != null){
                    job = (SourceJob)jjInfo.sourceJobMap().get(source);
                }
            }
            if (job == null){
			    job = compiler.sourceExtension().addJob(source);
            }
   
            boolean result = false;
		    result = compiler.sourceExtension().runToCompletion();
		
            if (!result) {
            
                throw new soot.CompilationDeathException(0, "Could not compile");
            }

        
            
            polyglot.ast.Node node = job.ast();

			return node;

		}
		catch (IOException e){
            return null;
		}

	}

}