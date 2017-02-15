/**
 *  Copyright 2012 Diego Ceccarelli
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package it.cnr.isti.hpc.dexter.spot;

import it.cnr.isti.hpc.dexter.entity.EntityMatch;
import it.cnr.isti.hpc.dexter.entity.EntityMatchList;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Represents a list of {@link SpotMatch}
 * 
 * @author Diego Ceccarelli, diego.ceccarelli@isti.cnr.it created on 03/ago/2012
 */
public class SpotMatchList extends ArrayList<SpotMatch> {

    private static final long serialVersionUID = 1L;

    /**
     * Logger for this class
     */
    private static final Logger logger = LoggerFactory.getLogger(SpotMatchList.class);

    @Override
    public boolean add(SpotMatch m) {
        logger.debug("adding spot {} ", m.spot.getMention());
        return super.add(m);

    }

    public void normalizeSpotProbabilities() {
        double totalProbability = 0;
        for (SpotMatch s : this) {
            totalProbability += s.getProbability();
        }
        for (SpotMatch s : this) {
            s.setProbability(s.getProbability() / totalProbability);
        }
    }

    public int index(Spot s) {
        int i = 0;
        for (SpotMatch em : this) {
            if (em.getSpot().equals(s))
                return i;
            i++;
        }
        return -1;

    }

    public EntityMatchList getEntities() {
        EntityMatchList eml = new EntityMatchList();
        for (SpotMatch match : this) {
            eml.addAll(match.getEntities());
        }
        return eml;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (SpotMatch m : this) {
            sb.append(m.spot.getMention()).append("\n");
            sb.append("\t").append("entities: [ ");
            for (EntityMatch e : m.getEntities()) {
                sb.append(e.toString()).append(" ");
            }
            sb.append(" ]\n");

        }
        return sb.toString();
    }

    public void sortByProbability() {
        Collections.sort(this, new ProbabilityComparator());
    }

    public void sortByStartPosition() {
        Collections.sort(this, new StartPositionComparator());
    }

    private class ProbabilityComparator implements Comparator<SpotMatch> {

        @Override
        public int compare(SpotMatch s1, SpotMatch s2) {
            if (s1.getProbability() > s2.getProbability())
                return -1;
            else
                return 1;
        }

    }

    private class StartPositionComparator implements Comparator<SpotMatch> {

        @Override
        public int compare(SpotMatch s1, SpotMatch s2) {
            return s1.getStart() - s2.getStart();
        }

    }

    private class EntityProbabilityComparator implements Comparator<EntityMatch> {

        @Override
        public int compare(EntityMatch e1, EntityMatch e2) {

            double e1score = e1.getScore() * e1.getSpotLinkProbability();

            double e2score = e2.getScore() * e2.getSpotLinkProbability();
            if (e1score > e2score)
                return -1;
            else
                return 1;

        }

    }

    public SpotMatchList removeOverlappings() {
        SpotMatchList filtered = new SpotMatchList();
        for (SpotMatch e : this) {
            boolean overlaps = false;
            for (SpotMatch e1 : this) {
                if (e1 != e) {
                    overlaps = e.overlaps(e1);
                    if (overlaps)
                        break;
                }

            }
            if (!overlaps) {
                filtered.add(e);
            }
        }
        int pointer = -1;
        for (SpotMatch e : filtered) {
            if (e.getStart() < pointer) {
                e.setStart(pointer + 1);
            }
            pointer = e.getEnd();
        }

        return filtered;

    }

}
