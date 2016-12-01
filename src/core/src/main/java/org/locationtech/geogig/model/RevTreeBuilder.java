/* Copyright (c) 2012-2014 Boundless and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/org/documents/edl-v10.html
 *
 * Contributors:
 * Gabriel Roldan (Boundless) - initial implementation
 */
package org.locationtech.geogig.model;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.SortedMap;
import java.util.concurrent.atomic.AtomicBoolean;

import org.eclipse.jdt.annotation.Nullable;
import org.locationtech.geogig.plumbing.HashObject;
import org.locationtech.geogig.storage.ObjectStore;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSortedMap;

/**
 * A builder for {@link RevTree} instances whose {@link Node nodes} are arranged following a
 * specific "clustering strategy" (e.g. to create a tree with the {@link CanonicalNodeNameOrder
 * canonical} structure, or some other node arrangement like an index on a specific attribute, etc).
 *
 * <p>
 * Since {@code RevTree} is an immutable data structure, a {@code RevTreeBuilder} must be used to
 * {@link #put(Node) add} or {@link #remove(String) remove} nodes until the tree can be
 * {@link #build() built}.
 * 
 * <p>
 * A {@code RevTreeBuilder} operates against an {@link ObjectStore}, onto which it'll save both the
 * resulting {@code RevTree} and any internal {@code RevTree} the built tree is to be split into. So
 * when {@link #build()} returns, the resulting {@code RevTree} is guaranteed to be fully stored on
 * the provided {@code ObjectStore}.
 * 
 * <p>
 * When a
 */
public interface RevTreeBuilder {

    public RevTreeBuilder put(Node node);

    public RevTreeBuilder remove(String featureId);

    public RevTree build();

    /**
     * Factory method to create a tree builder that clusters subtrees and nodes according to
     * {@link CanonicalNodeNameOrder}
     */
    static RevTreeBuilder canonical(final ObjectStore store) {
        return RevTreeBuilder.canonical(store, RevTree.EMPTY);
    }

    ////
    // this is a temporary meassure to defaulting to use the legacy tree builder but allowing to
    // specify using the new through a System property. Run CLI with -Dtreebuilder.new=true (e.g. in
    // JAVA_OPTS).
    ////
    static final AtomicBoolean notified = new AtomicBoolean();

    /**
     * Factory method to create a tree builder that clusters subtrees and nodes according to
     * {@link CanonicalNodeNameOrder}, and whose internal structure starts by matching the provided
     * {@code original} tree.
     */
    static RevTreeBuilder canonical(final ObjectStore store, final RevTree original) {
        checkNotNull(store);
        checkNotNull(original);
        RevTreeBuilder builder;
        final boolean USE_NEW_BUILDER = Boolean.getBoolean("treebuilder.new");
        if (USE_NEW_BUILDER) {
            if (!notified.getAndSet(true)) {
                System.err.println(
                        "Using experimental tree builder " + CanonicalTreeBuilder.class.getName());
            }
            builder = new CanonicalTreeBuilder(store, original);
        } else {
            builder = new LegacyTreeBuilder(store, original);
        }
        return builder;
    }

    static RevTree build(final long size, final int childTreeCount,
            @Nullable ImmutableList<Node> trees, @Nullable ImmutableList<Node> features,
            @Nullable ImmutableSortedMap<Integer, Bucket> buckets) {

        ObjectId id = HashObject.hashTree(trees, features, buckets);
        return RevTreeImpl.create(id, size, childTreeCount, trees, features, buckets);
    }

    /**
     * Creates a tree with the given id and contents, no questions asked.
     * <p>
     * Be careful when using this method instead of {@link #build()}. {@link #build()} will compute
     * the appropriate id for the tree given its contents as mandated by {@link HashObject}, whilst
     * this method will create the tree as given, even if the id is not the one that would result
     * from properly computing it.
     * 
     * @param id
     * @param size
     * @param childTreeCount
     * @param trees
     * @param features
     * @param buckets
     * @return
     */
    static RevTree create(final ObjectId id, final long size, final int childTreeCount,
            @Nullable ImmutableList<Node> trees, @Nullable ImmutableList<Node> features,
            @Nullable SortedMap<Integer, Bucket> buckets) {

        ImmutableSortedMap<Integer, Bucket> immutableBuckets = buckets == null ? null
                : ImmutableSortedMap.copyOfSorted(buckets);

        return RevTreeImpl.create(id, size, childTreeCount, trees, features, immutableBuckets);

    }
}