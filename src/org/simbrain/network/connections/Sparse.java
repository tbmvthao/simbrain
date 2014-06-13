/*
 * Copyright (C) 2005,2007 The Authors.  See http://www.simbrain.net/credits
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package org.simbrain.network.connections;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import org.simbrain.network.core.Network;
import org.simbrain.network.core.Neuron;
import org.simbrain.network.core.Synapse;
import org.simbrain.network.groups.SynapseGroup;
import org.simbrain.util.math.SimbrainMath;

import umontreal.iro.lecuyer.randvar.BinomialGen;

/**
 * Connect neurons sparsely.
 * 
 * @author Zach Tosi
 */
public class Sparse extends DensityBasedConnector {

    /**
     * The default preference as to whether or not self connections are allowed.
     */
    public static boolean DEFAULT_SELF_CONNECT_PREF;

    /**
     * Sets the default behavior concerning whether or not the number of
     * efferents of each source neurons should be equalized.
     */
    public static boolean DEFAULT_FF_PREF;

    /** The default sparsity (between 0 and 1). */
    public static double DEFAULT_CONNECTION_DENSITY = 0.1;

    /**
     * The overall the percentage of possible connections which are actually
     * made (on [0, 1]).
     */
    private double connectionDensity = DEFAULT_CONNECTION_DENSITY;

    /**
     * Whether or not a synapse in which the source and target neuron are the
     * same neuron is allowed.
     */
    private boolean allowSelfConnect = DEFAULT_SELF_CONNECT_PREF;

    /**
     * Whether or not each source neuron is given an equal number of efferent
     * synapses.
     */
    private boolean equalizeEfferents = DEFAULT_FF_PREF;

    /**
     * A map of permutations governing in what order connections to target
     * neurons will be added if the connection density is raised for each source
     * neuron. Maps which index of target neuron will be the next to be given a
     * connection, or in what order connections are removed for each source
     * neuron if density is lowered.
     */
    private int[][] sparseOrdering;

    /**
     * If efferent synapses are not equalized among source neurons, this array
     * contains the number of possible target neurons a given source neuron is
     * connected to.
     */
    private int[] currentOrderingIndices;

    /** The source neurons. */
    private Neuron[] sourceNeurons;

    /** The target neurons. */
    private Neuron[] targetNeurons;

    /** The synapse group associated with this connection object. */
    private SynapseGroup synapseGroup;

    /** {@inheritDoc} */
    public Sparse() {
        super();
    }

    /**
     * See super class description.
     * 
     * @param network
     *            network with neurons to be connected.
     * @param neurons
     *            source neurons.
     * @param neurons2
     *            target neurons.
     */
    public Sparse(double sparsity, boolean equalizeEfferents,
            boolean allowSelfConnect) {
        this.connectionDensity = sparsity;
        this.equalizeEfferents = equalizeEfferents;
        this.allowSelfConnect = allowSelfConnect;
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return "Sparse";
    }

    /**
     * Connects two lists of neurons with synapses assigning connections between
     * source and target neurons randomly in such a way that results in
     * "sparsity" percentage of possible connections being created.
     * 
     * @param sourceNeurons
     * @param targetNeurons
     * @param sparsity
     * @param allowSelfConnection
     * @param equalizeEfferents
     * @param looseSynapses
     * @return
     */
    public static List<Synapse> connectSparse(List<Neuron> sourceNeurons,
            List<Neuron> targetNeurons, double sparsity,
            boolean allowSelfConnection, boolean equalizeEfferents,
            boolean looseSynapses) {
        boolean recurrent = ConnectionUtilities.testRecurrence(sourceNeurons,
                targetNeurons);
        Neuron source;
        Neuron target;
        Synapse synapse;
        ArrayList<Synapse> syns = new ArrayList<Synapse>();
        Random rand = new Random();
        if (equalizeEfferents) {
            ArrayList<Integer> targetList = new ArrayList<Integer>();
            ArrayList<Integer> tListCopy;
            for (int i = 0; i < targetNeurons.size(); i++) {
                targetList.add(i);
            }
            int numSyns;
            if (!allowSelfConnection && sourceNeurons == targetNeurons) {
                numSyns = (int) (sparsity * sourceNeurons.size() * (targetNeurons
                        .size() - 1));
            } else {
                numSyns = (int) (sparsity * sourceNeurons.size() * targetNeurons
                        .size());
            }
            int synsPerSource = numSyns / sourceNeurons.size();
            int targStart = 0;
            int targEnd = synsPerSource;
            if (synsPerSource > numSyns / 2) {
                synsPerSource = numSyns - synsPerSource;
                targStart = synsPerSource;
                targEnd = targetList.size();
            }

            for (int i = 0; i < sourceNeurons.size(); i++) {
                source = sourceNeurons.get(i);
                if (!allowSelfConnection && recurrent) {
                    tListCopy = new ArrayList<Integer>();
                    for (int k = 0; k < targetList.size(); k++) {
                        if (k == i) // Exclude oneself as a possible target
                            continue;
                        tListCopy.add(targetList.get(k));
                    }
                    randShuffleK(tListCopy, synsPerSource, rand);
                } else {
                    randShuffleK(targetList, synsPerSource, rand);
                    tListCopy = targetList;
                }

                for (int j = targStart; j < targEnd; j++) {
                    target = targetNeurons.get(tListCopy.get(j));
                    synapse = new Synapse(source, target);
                    if (looseSynapses) {
                        source.getNetwork().addSynapse(synapse);
                    }
                    syns.add(synapse);
                }
            }
        } else {
            for (int i = 0; i < sourceNeurons.size(); i++) {
                for (int j = 0; j < targetNeurons.size(); j++) {
                    if (!allowSelfConnection && recurrent && i == j) {
                        continue;
                    } else {
                        if (Math.random() < sparsity) {
                            source = sourceNeurons.get(i);
                            target = targetNeurons.get(j);
                            synapse = new Synapse(source, target);
                            if (looseSynapses) {
                                source.getNetwork().addSynapse(synapse);
                            }
                            syns.add(synapse);
                        }
                    }
                }

            }
        }
        return syns;

    }

    /**
     * 
     */
    public void connectNeurons(SynapseGroup synapseGroup) {
        this.synapseGroup = synapseGroup;
        boolean recurrent = synapseGroup.isRecurrent();
        int numSrc = synapseGroup.getSourceNeurons().size();
        int numTar = synapseGroup.getTargetNeurons().size();
        sourceNeurons = synapseGroup.getSourceNeurons().toArray(
                new Neuron[numSrc]);
        targetNeurons = recurrent ? sourceNeurons : synapseGroup
                .getTargetNeurons().toArray(new Neuron[numTar]);
        generateSparseOrdering(recurrent);
        if (equalizeEfferents) {
            connectEqualized(synapseGroup);
        } else {
            connectRandom(synapseGroup);
        }

    }

    /**
     * Populates the synapse group with synapses by making individual synaptic
     * connections between the neurons in the synapse group's source and target
     * groups. These synapses are initialized with default attributes and zero
     * strength. Each source neuron will have exactly the same number of
     * efferent synapses. This number being whichever satisfies the constraints
     * given by the sparsity and whether or not the synapse group is recurrent
     * and self connections are allowed.
     * 
     * @param synapseGroup
     */
    private void connectEqualized(SynapseGroup synapseGroup) {
        currentOrderingIndices = new int[sourceNeurons.length];
        int numConnectsPerSrc;
        int expectedNumSyns;
        if (synapseGroup.isRecurrent() && !allowSelfConnect) {
            numConnectsPerSrc = (int) (connectionDensity * (sourceNeurons.length - 1));
        } else {
            numConnectsPerSrc = (int) (connectionDensity * targetNeurons.length);
        }
        expectedNumSyns = numConnectsPerSrc * sourceNeurons.length;
        synapseGroup.preAllocateSynapses(expectedNumSyns);
        for (int i = 0, n = sourceNeurons.length; i < n; i++) {
            currentOrderingIndices[i] = numConnectsPerSrc;
            Neuron src = sourceNeurons[i];
            Neuron tar;
            for (int j = 0; j < numConnectsPerSrc; j++) {
                tar = targetNeurons[sparseOrdering[i][j]];
                Synapse s = new Synapse(src, tar);
                synapseGroup.addNewSynapse(s);
            }
        }
    }

    /**
     * Populates the synapse group with synapses by making individual synaptic
     * connections between the neurons in the synapse group's source and target
     * groups. These synapses are initialized with default attributes and zero
     * strength. The number of efferent synapses assigned to each source neuron
     * is drawn from a binomial distribution with a mean of
     * NumberOfTargetNeurons * sparsity
     * 
     * @param synapseGroup
     */
    private void connectRandom(SynapseGroup synapseGroup) {
        currentOrderingIndices = new int[sourceNeurons.length];
        int numTars = synapseGroup.isRecurrent() && !allowSelfConnect ? (sourceNeurons.length - 1)
                : targetNeurons.length;
        synapseGroup
                .preAllocateSynapses((int) (sourceNeurons.length * numTars * connectionDensity));
        for (int i = 0, n = sourceNeurons.length; i < n; i++) {
            currentOrderingIndices[i] = BinomialGen.nextInt(
                    SimbrainMath.DEFAULT_RANDOM_STREAM, numTars,
                    connectionDensity);
            Neuron src = sourceNeurons[i];
            Neuron tar;
            for (int j = 0; j < currentOrderingIndices[i]; j++) {
                tar = targetNeurons[sparseOrdering[i][j]];
                Synapse s = new Synapse(src, tar);
                synapseGroup.addNewSynapse(s);
            }
        }

    }

    /**
     * 
     * @param recurrent
     */
    private void generateSparseOrdering(boolean recurrent) {
        int srcLen = sourceNeurons.length;
        if (recurrent && !allowSelfConnect) {
            int tarLen = targetNeurons.length - 1;
            sparseOrdering = new int[sourceNeurons.length][tarLen];
            for (int i = 0; i < srcLen; i++) {
                sparseOrdering[i] = SimbrainMath.randPermuteWithExclusion(0,
                        tarLen + 1, i);
            }
        } else {
            int tarLen = targetNeurons.length;
            sparseOrdering = new int[sourceNeurons.length][tarLen];
            for (int i = 0; i < srcLen; i++) {
                sparseOrdering[i] = SimbrainMath.randPermute(0, tarLen);
            }
        }
    }

    /**
     * Randomly shuffles k integers in a list. The first k elements are randomly
     * swapped with other elements in the list. This method will alter the list
     * passed to it, so situations where this would be undesirable should pass
     * this method a copy.
     * 
     * @param inds
     *            a list of integers. This methods WILL shuffle inds, so pass a
     *            copy unless inds being shuffled is not a problem.
     * @param k
     *            how many elements will be shuffled
     * @param rand
     *            a random number generator
     */
    public static void randShuffleK(ArrayList<Integer> inds, int k, Random rand) {
        for (int i = 0; i < k; i++) {
            Collections.swap(inds, i, rand.nextInt(inds.size()));
        }
    }

    /**
     * 
     * @param newSparsity
     * @param returnRemoved
     * @return
     */
    public List<Synapse> removeToSparsity(double newSparsity,
            boolean returnRemoved) {
        if (newSparsity >= connectionDensity) {
            throw new IllegalArgumentException("Cannot 'removeToSparsity' to"
                    + " a higher connectivity density.");
        }
        Network net = sourceNeurons[0].getNetwork();
        int removeTotal = (synapseGroup.size() - (int) (newSparsity * getMaxPossibleConnections()));
        List<Synapse> removeList = null;
        if (returnRemoved) {
            removeList = new ArrayList<Synapse>((int) (removeTotal / 0.75));
        }
        if (equalizeEfferents) {
            int curNumConPerSource = synapseGroup.size() / sourceNeurons.length;
            int removePerSource = removeTotal / sourceNeurons.length;
            int finalNumConPerSource = curNumConPerSource - removePerSource;
            for (int i = 0, n = sourceNeurons.length; i < n; i++) {
                for (int j = curNumConPerSource - 1; j >= finalNumConPerSource; j--) {
                    Synapse toRemove = Network.getSynapse(sourceNeurons[i],
                            targetNeurons[sparseOrdering[i][j]]);
                    if (returnRemoved) {
                        removeList.add(toRemove);
                    }
                    net.removeSynapse(toRemove);
                }
                currentOrderingIndices[i] = finalNumConPerSource;
            }
        } else {
            for (int i = 0, n = sourceNeurons.length; i < n; i++) {
                int numToRemove = BinomialGen.nextInt(
                        SimbrainMath.DEFAULT_RANDOM_STREAM,
                        currentOrderingIndices[i],
                        1 - (newSparsity / connectionDensity));
                for (int j = currentOrderingIndices[i] - 1; j >= currentOrderingIndices[i]
                        - numToRemove; j--) {
                    Synapse toRemove = Network.getSynapse(sourceNeurons[i],
                            targetNeurons[sparseOrdering[i][j]]);
                    if (returnRemoved) {
                        removeList.add(toRemove);
                    }
                    net.removeSynapse(toRemove);
                }
                currentOrderingIndices[i] -= numToRemove;
            }
        }
        this.connectionDensity = newSparsity;
        return removeList;
    }

    /**
     * 
     * @param newSparsity
     * @return
     */
    public List<Synapse> addToSparsity(double newSparsity) {
        if (newSparsity <= connectionDensity) {
            throw new IllegalArgumentException("Cannot 'addToSparsity' to"
                    + " a lower connectivity density.");
        }
        int addTotal = ((int) (newSparsity * getMaxPossibleConnections()) - synapseGroup
                .size());
        List<Synapse> addList = new ArrayList<Synapse>((int) (addTotal / 0.75));
        if (equalizeEfferents) {
            int curNumConPerSource = synapseGroup.size() / sourceNeurons.length;
            int addPerSource = addTotal / sourceNeurons.length;
            int finalNumConPerSource = curNumConPerSource + addPerSource;
            if (finalNumConPerSource > sparseOrdering[0].length) {
                finalNumConPerSource = sparseOrdering[0].length;
            }
            for (int i = 0, n = sourceNeurons.length; i < n; i++) {
                for (int j = curNumConPerSource; j < finalNumConPerSource; j++) {
                    Synapse toAdd = new Synapse(sourceNeurons[i],
                            targetNeurons[sparseOrdering[i][j]]);
                    addList.add(toAdd);
                }
                currentOrderingIndices[i] = finalNumConPerSource;
            }
        } else {
            for (int i = 0, n = sourceNeurons.length; i < n; i++) {
                int numToAdd = BinomialGen.nextInt(
                        SimbrainMath.DEFAULT_RANDOM_STREAM,
                        currentOrderingIndices[i],
                        1 - (connectionDensity / newSparsity));
                int finalNumConPerSource = currentOrderingIndices[i] + numToAdd;
                if (finalNumConPerSource > sparseOrdering[i].length) {
                    finalNumConPerSource = sparseOrdering[i].length;
                }
                for (int j = currentOrderingIndices[i]; j < finalNumConPerSource; j++) {
                    Synapse toAdd = new Synapse(sourceNeurons[i],
                            targetNeurons[sparseOrdering[i][j]]);
                    addList.add(toAdd);
                }
                currentOrderingIndices[i] = finalNumConPerSource;
            }
        }
        for (Synapse s : addList) {
            synapseGroup.addNewSynapse(s);
        }
        this.connectionDensity = newSparsity;
        return addList;
    }

    public int getMaxPossibleConnections() {
        if (allowSelfConnect || !synapseGroup.isRecurrent()) {
            return sourceNeurons.length * targetNeurons.length;
        } else {
            return sourceNeurons.length * (sourceNeurons.length - 1);
        }
    }

    public boolean isEqualizeEfferents() {
        return equalizeEfferents;
    }

    public void setEqualizeEfferents(boolean equalizeEfferents) {

        this.equalizeEfferents = equalizeEfferents;
    }

    /** {@inheritDoc} */
    public void setAllowSelfConnection(boolean allowSelfConnect) {
        this.allowSelfConnect = allowSelfConnect;
    }

    /** {@inheritDoc} */
    @Override
    public double getConnectionDensity() {
        return connectionDensity;
    }

    /** {@inheritDoc} */
    @Override
    public Collection<Synapse> setConnectionDensity(double connectionDensity) {
        if (sparseOrdering == null) {
            this.connectionDensity = connectionDensity;
        } else {
            if (connectionDensity > this.connectionDensity) {
                return addToSparsity(connectionDensity);
            } else if (connectionDensity < this.connectionDensity) {
                return removeToSparsity(connectionDensity, true);
            }
        }
        return null;
    }

}
