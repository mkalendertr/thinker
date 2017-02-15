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

import java.util.HashSet;
import java.util.Set;

/**
 * This class computes Jaccard metrics. The Jaccard similarity is the number of
 * properties two objects have in common divided by the total number of
 * properties possessed by the two objects. Two objects with the same properties
 * will have a similarity of 1.0. Two objects sharing no properties will have a
 * similarity of 0.0. Jaccard distance is one minus the Jaccard similarity.
 * 
 * @author Keith
 * 
 */
public class JaccardCalculator {

	public JaccardCalculator() {
		super();
	}

	/**
	 * This method computes Jaccard distance. The Jaccard similarity is the
	 * number of properties two objects have in common divided by the total
	 * number of properties possessed by the two objects. Jaccard distance is
	 * one minus the Jaccard similarity.
	 * 
	 * @param properties1
	 *            the properties of the first object
	 * @param properties2
	 *            the properties of the second object
	 * @return Jaccard distance between 0 (identical properties) and 1 (no
	 *         shared properties)
	 */
	public Double calculateDistance(Set<String> properties1,
			Set<String> properties2) {
		Double distance;
		double similarity = calculateSimilarity(properties1, properties2);
		distance = 1.0 - similarity;
		return distance;
	}

	/**
	 * This method computes Jaccard similarity. The Jaccard similarity is the
	 * number of properties two objects have in common divided by the total
	 * number of properties possessed by the two objects.
	 * 
	 * @param properties1
	 *            the properties of the first object
	 * @param properties2
	 *            the properties of the second object
	 * @return Jaccard similarity between 0 (no shared properties) and 1
	 *         (identical properties)
	 */
	public static double calculateSimilarity(Set<String> properties1,
			Set<String> properties2) {
		Set<String> intersection = new HashSet<String>(properties1);
		intersection.retainAll(properties2);
		Set<String> union = new HashSet<String>(properties1);
		union.addAll(properties2);

		int intersectionSize = intersection.size();
		int unionSize = union.size();
		double similarity = 0.0;

		if (unionSize != 0) {
			similarity = 1.0 * intersectionSize / unionSize;
		}
		return similarity;
	}

}