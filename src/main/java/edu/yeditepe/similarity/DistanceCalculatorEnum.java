/* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
Copyright (c) 2010, Keith Cassell
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are
met:

    * Redistributions of source code must retain the above copyright
      notice, this list of conditions and the following disclaimer.
    * Redistributions in binary form must reproduce the above
      copyright notice, this list of conditions and the following 
      disclaimer in the documentation and/or other materials
      provided with the distribution.
    * Neither the name of the Victoria University of Wellington
      nor the names of its contributors may be used to endorse or
      promote products derived from this software without specific
      prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
"AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
(INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * */

package edu.yeditepe.similarity;

/**
 * Identifiers to specify which distance calculator to use.
 * These should be consistent with the identifiers used in extc.properties
 * @author kcassell
 *
 */
public enum DistanceCalculatorEnum {
	ClientDistance,
	Czibula,
	GoogleDistance,
	Identifier,
	IntraClass,
	JDeodorant,
	Levenshtein,
	LocalNeighborhood,
	LSA, // Latent Semantic Analysis
	Simon,
	VectorSpaceModel;
	
	/**
	 * Some distance calculators calculate distance based on properties
	 * of a class that require Eclipse handles to calculate, while others
	 * merely require the member identifier.
	 * @param calcType the calculator type
	 * @return true when the calculator requires handles; false if it only
	 * requires identifiers
	 */
	public static boolean usesHandles(DistanceCalculatorEnum calcType) {
		boolean useHandles =
			Czibula.equals(calcType)
			|| JDeodorant.equals(calcType)
			|| LocalNeighborhood.equals(calcType)
			|| Simon.equals(calcType)
			|| VectorSpaceModel.equals(calcType);
		return useHandles;
	}
}
