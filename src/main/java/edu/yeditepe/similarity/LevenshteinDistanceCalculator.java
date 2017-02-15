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
 * This class calculates a normalized Levenshtein distance (a.k.a edit distance) between two strings. The raw
 * Levenshtein distance is the number of letter additions, deletions, and/or substitutions needed to transform one
 * string into another. The normalization done here is to put the score in a [0, 1] interval by dividing the raw score
 * by the length of the longest string in the comparison.
 * 
 * @author Keith
 * 
 */
public class LevenshteinDistanceCalculator {

    /**
     * Calculates the distance between the identifiers. We ignore the case of the letters in the comparison.
     * 
     * @return between 0 (identical) and 1 (completely different)
     */
    public static Double calculateDistance(String s1, String s2) {
        Double result = 1.0;

        if (s1 != null && s2 != null) {
            result = calculateNormalizedDistance(s1, s2);
        }
        return result;
    }

    /**
     * Calculates the normalized Levenshtein distance. We ignore the case of the letters in the comparison.
     * 
     * @return between 0 (identical) and 1 (completely different)
     */
    protected static Double calculateNormalizedDistance(String s1, String s2) {
        Double lDistance = 0.0;
        int max = Math.max(s1.length(), s2.length());
        if (max > 0) {
            s1 = s1.toLowerCase();
            s2 = s2.toLowerCase();
            int distance = Levenshtein.distance(s1, s2);
            lDistance = distance * 1.0 / max;
        }
        return lDistance;
    }

    public DistanceCalculatorEnum getType() {
        return DistanceCalculatorEnum.Levenshtein;
    }

}
