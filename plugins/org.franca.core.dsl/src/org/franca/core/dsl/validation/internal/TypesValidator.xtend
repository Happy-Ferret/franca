/*******************************************************************************
 * Copyright (c) 2012 itemis AG (http://www.itemis.de).
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.franca.core.dsl.validation.internal;

import org.eclipse.emf.ecore.EObject
import org.eclipse.emf.ecore.EStructuralFeature
import org.franca.core.contracts.IssueCollector
import org.franca.core.contracts.TypeIssue
import org.franca.core.contracts.TypeSystem
import org.franca.core.framework.FrancaHelpers
import org.franca.core.franca.FConstantDef
import org.franca.core.franca.FExpression
import org.franca.core.franca.FInitializer
import org.franca.core.franca.FTypeRef
import org.franca.core.franca.FCompoundInitializer
import org.franca.core.franca.FStructType
import org.franca.core.franca.FBracketInitializer
import org.franca.core.franca.FArrayType
import org.franca.core.franca.FMapType
import org.franca.core.franca.FEnumerator
import org.franca.core.franca.FBasicTypeId

import static org.franca.core.franca.FrancaPackage$Literals.*
import static extension org.franca.core.FrancaModelExtensions.*
import static extension org.franca.core.framework.FrancaHelpers.*
import static extension org.franca.core.utils.ExpressionEvaluator.*
import java.math.BigInteger
import org.franca.core.dsl.validation.internal.CyclicDependenciesDetector
import org.franca.core.franca.FDeclaration

class TypesValidator {
	
	
	
	def static checkConstantType (ValidationMessageReporter reporter, FDeclaration declaration) {
		//TODO check whether this is needed anymore, I deleted this previously (new)
		checkConstantRHS(declaration.rhs, declaration.type,
			reporter, declaration, FDECLARATION__RHS, -1
		)
	}
	

	def static boolean checkConstantType (ValidationMessageReporter reporter, FConstantDef constantDef) {
//		
//		If the concrete type of the constant should once get necessary to return, then an artificial interval with
//      borders equal to the constants value should be created and returned.
//		
		val typeLHS= constantDef.type
		val typeRHS = checkConstantRHS(constantDef.rhs, typeLHS, reporter, constantDef, FCONSTANT_DEF__RHS, -1)
						
		//evaluate actual value for numbers here
		if (typeRHS != null) {
			if (typeRHS.isNumber) {
				
				// A cycle would cause a stack overflow, so abort the evaluation in that case
				if (CyclicDependenciesDetector::hasCycle(constantDef)) {
					return false
				}
						
				val valueRHS = constantDef.rhs.evaluate;
				val boolean fits =
						if (typeLHS.isDouble) {
							typeRHS.isDouble || typeRHS.isFloat
						} else if (typeLHS.isFloat) {
							typeRHS.isFloat
//						} else if (typeRHS.isBigDecimal) {
//							throw new IllegalArgumentException("BigDecimal is currently not supported.")
						} else if (typeLHS.isInteger && typeRHS.isInteger) {
							val biRHS = if (valueRHS instanceof BigInteger) valueRHS as BigInteger else BigInteger.valueOf((valueRHS as Number).longValue)
							var actualInterval = typeLHS.actualInterval;
							val actualInteger = typeLHS.actualPredefined;
							if (actualInterval == null) {
								actualInterval = TypeSystem::integerMapping.get(actualInteger);
							};
							(actualInterval.lowerBound == null || actualInterval.lowerBound.compareTo(biRHS) <= 0) &&
							(actualInterval.upperBound == null || actualInterval.upperBound.compareTo(biRHS) >= 0) 								
						} else {
							false
						}
				if (!fits) {
					reporter.reportError("Value " + valueRHS + " of type " + typeRHS.typeString + " is not assignable to " + typeLHS.typeString, constantDef, FCONSTANT_DEF__RHS)
					return false
				} else {
					return true
				}
			} else if (!TypeSystem::isAssignableTo(typeRHS, typeLHS)) {
				reporter.reportError(typeRHS.typeString + " is not assignable to " + typeLHS.typeString, constantDef, FCONSTANT_DEF__RHS)
				return false				
			}
		}
		return true
	}

	def static void checkEnumValueType (ValidationMessageReporter reporter, FEnumerator enumerator) {
		val type = checkExpression(reporter, enumerator.value, enumerator, FENUMERATOR__VALUE, -1);
		
		// for backward compatibility, we allow Strings as enum values
		// TODO: remove this case when the deprecated feature is removed
		if (FrancaHelpers::isBasicType(type, FBasicTypeId::STRING)) {
			// String values for enumerators are deprecated
			reporter.reportWarning(
				"Deprecated: String value for enumerator (use integer expression instead).",
				enumerator, FENUMERATOR__VALUE)
			
		} else if (! type.isInteger) {
			reporter.reportError("expected type Integer, but was " + type.typeString, enumerator, FENUMERATOR__VALUE)
		}
	}

	def private static FTypeRef checkConstantRHS (
		FExpression rhs,
		FTypeRef typeLHS,
		ValidationMessageReporter reporter,
		EObject ctxt,
		EStructuralFeature feature,
		int index
	) {
		switch (rhs) {
			//TODO initializer should also be useable for variable initialization, shouldn't they?
			FInitializer: {
				checkInitializer(rhs, typeLHS, reporter, ctxt, feature, index)
			}
			FExpression: {
				checkExpression(reporter, rhs, ctxt, feature, index)
			}
		}
	}

	def private static dispatch FTypeRef checkInitializer (
		FBracketInitializer rhs,
		FTypeRef type,
		ValidationMessageReporter reporter,
		EObject ctxt,
		EStructuralFeature feature,
		int index
	) {
		if (! (type.isArray || type.isMap)) {
			reporter.reportError(
					"invalid initializer in constant definition (expected " +
						FrancaHelpers::getTypeString(type) + ")",
					ctxt, feature);
			return null;
		}
		
		if (type.isArray) {
			val t = type.actualDerived as FArrayType
			for(e : rhs.elements) {
				val idx = rhs.elements.indexOf(e)
				if (e.second!=null) {
					reporter.reportError(
							"invalid initializer for array element",
							rhs, FBRACKET_INITIALIZER__ELEMENTS, idx);
				} else {
					checkConstantRHS(e.first,
						t.elementType,
						reporter, e, FELEMENT_INITIALIZER__FIRST, -1
					)
				}
			}
		} else if (type.isMap) {
			val t = type.actualDerived as FMapType
			for(e : rhs.elements) {
				val idx = rhs.elements.indexOf(e)
				if (e.second==null) {
					reporter.reportError(
							"invalid initializer for map element",
							rhs, FBRACKET_INITIALIZER__ELEMENTS, idx);
				} else {
					checkConstantRHS(e.first,
						t.keyType,
						reporter, e, FELEMENT_INITIALIZER__FIRST, -1
					)
					checkConstantRHS(e.second,
						t.valueType,
						reporter, e, FELEMENT_INITIALIZER__SECOND, -1
					)
				}
			}
		}
		return type
	}
	
	def private static dispatch FTypeRef checkInitializer (
		FCompoundInitializer rhs,
		FTypeRef type,
		ValidationMessageReporter reporter,
		EObject ctxt,
		EStructuralFeature feature,
		int index
	) {
		if (! type.isCompound) {
			reporter.reportError(
					"invalid compound initializer in constant definition (expected " +
						FrancaHelpers::getTypeString(type) + ")",
					ctxt, feature, index);
			return null;
		}
		
		if (type.isStruct) {
			val t = type.actualDerived as FStructType
			val elems = t.getAllElements
			
			// check if there are initializers for all struct elements
			val fields = rhs.elements.map[element]
			for(e : elems) {
				if (! fields.contains(e)) {
					reporter.reportError(
							"initializer for element '" + e.name + "' missing",
							ctxt, feature, index);
				}
			}
			
			// check the types for all initializers
			for(e : rhs.elements) {
				checkConstantRHS(e.value, e.element.type,
					reporter, e, FFIELD_INITIALIZER__VALUE, -1
				)
			}
		} else if (type.isUnion) {
			if (rhs.elements.size!=1) {
				reporter.reportError(
						"union initializer must have exactly one element",
						ctxt, feature, index);
			}

			// check type
			val e = rhs.elements.get(0)
			checkConstantRHS(e.value, e.element.type,
				reporter, e, FFIELD_INITIALIZER__VALUE, 0
			)
		}
		
		return type
	}

	def private static dispatch FTypeRef checkInitializer (
		FInitializer rhs,
		FTypeRef type,
		ValidationMessageReporter reporter,
		EObject ctxt,
		EStructuralFeature feature,
		int index
	) {
		throw new RuntimeException("Unknown FInitializer type " + rhs.class.toString)
	}
	
	/**
	 * Check the validity of an expression. The expression gets processed according to the abstract syntax tree. Any
	 * error contained in independent branches of that tree will be reported.
	 * <p>
	 * No error in any part of the tree implies that the resulting type of the tree could be evaluated, too. In that
	 * case this type will be returned. Otherwise this function will return {@code null}
	 *  
	 * @param reporter The reporter that shall get used to report any issues contained in {@code expr}
	 * @param expr the expression which should be checked
	 * @param loc The EObject that contains the expression. This should be the root to mark any top level error within
	 *            the expression.
	 * @param feat The feature of the meta class of {@code loc} which defines the reference to the expression
	 * @param index The index of the expression indicating its position within a list of expressions, if {@code feat}
	 *              is a list of expressions. Will be ignored if the expression is not part of a list.  
	 */
	def public static FTypeRef checkExpression (
			ValidationMessageReporter reporter,
			FExpression expr,
			EObject loc, EStructuralFeature feat, int index)
	{
		val issues = new IssueCollector
		val ts = new TypeSystem(issues)
		val type = ts.checkType(expr, loc, feat)
		
		ValidationHelpers::reportExpressionIssues(issues, reporter, loc, feat, type==null);

		type
	}
}
