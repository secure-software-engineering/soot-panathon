/* Soot - a J*va Optimization Framework
 * Copyright (C) 2008 Eric Bodden
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the
 * Free Software Foundation, Inc., 59 Temple Place - Suite 330,
 * Boston, MA 02111-1307, USA.
 */
package soot;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import soot.javaToJimple.IInitialResolver;
import soot.JastAddJ.BodyDecl;
import soot.JastAddJ.CompilationUnit;
import soot.JastAddJ.ConstructorDecl;
import soot.JastAddJ.MethodDecl;
import soot.JastAddJ.Program;
import soot.JastAddJ.TypeDecl;

public class JastAddInitialResolver implements IInitialResolver {

    public JastAddInitialResolver(soot.Singletons.Global g){}

    public static JastAddInitialResolver v() {
        return soot.G.v().soot_JastAddInitialResolver();
    }
	
	protected Map<String,CompilationUnit> classNameToCU = new HashMap<String, CompilationUnit>();
	
	public void formAst(String fullPath, List<String> locations, String className) {
	      Program program = SootResolver.v().getProgram();
	      program.addSourceFile(fullPath);
	      int i = program.getNumCompilationUnit() - 1;
	      while(i >= 0 && !fullPath.equals(program.getCompilationUnit(i).pathName()))
	          i--;
	      if(i >= 0) {
	          CompilationUnit u = program.getCompilationUnit(i);
	          u.jimplify1phase1();
	          u.jimplify1phase2();
	          if(classNameToCU.containsKey(className)) {
	              throw new IllegalStateException();
	          }
	          classNameToCU.put(className, u);
	      }
	}

	public Dependencies resolveFromJavaFile(SootClass sc) {
		Dependencies deps = new Dependencies(); 
		for (SootMethod m : sc.getMethods()) {
			m.setSource(new MethodSource() {
				public Body getBody(SootMethod m, String phaseName) {
					CompilationUnit u = classNameToCU.get(m.getDeclaringClass().getName());
					soot.JastAddJ.List<TypeDecl> typeDeclList = u.getTypeDeclList();
					for (TypeDecl typeDecl : typeDeclList) {
						soot.JastAddJ.List<BodyDecl> bodyDeclList = typeDecl.getBodyDeclList();
						for (BodyDecl bodyDecl : bodyDeclList) {
							if(bodyDecl instanceof MethodDecl) {
								MethodDecl methodDecl = (MethodDecl) bodyDecl;
								if(m.equals(methodDecl.sootMethod))
									methodDecl.jimplify2();
							} else if(bodyDecl instanceof ConstructorDecl) {
								ConstructorDecl constrDecl = (ConstructorDecl) bodyDecl;
								if(m.equals(constrDecl.sootMethod))
									constrDecl.jimplify2();
							}
						}
					}					
					return m.getActiveBody();
				}
			});
			CompilationUnit u = classNameToCU.get(m.getDeclaringClass().getName());
		  	u.collectTypesToHierarchy(deps.typesToHierarchy);
		  	u.collectTypesToSignatures(deps.typesToSignature);
		}
		
        return deps;
	}

}
