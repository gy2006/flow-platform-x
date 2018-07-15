/*
 * Copyright 2018 fir.im
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.flowci.tree;

import com.flowci.domain.VariableMap;
import com.flowci.domain.node.Node;
import com.flowci.domain.node.NodePath;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import lombok.Getter;

/**
 * @author yang
 */
public class NodeTree {

    private final static int DEFAULT_SIZE = 20;

    /**
     * Create node tree from Node object
     * @param root
     * @return
     */
    public static NodeTree create(Node root) {
        return new NodeTree(root);
    }

    public static NodeTree create(String yml) {
        Node root = YmlParser.load(yml);
        return new NodeTree(root);
    }

    private final Map<NodePath, NodeWithIndex> cached = new HashMap<>(DEFAULT_SIZE);

    @Getter
    private final List<Node> ordered = new ArrayList<>(DEFAULT_SIZE);

    @Getter
    private final VariableMap sharedContext = new VariableMap();

    @Getter
    private Node root;

    public NodeTree(Node root) {
        buildTree(root);
        ordered.remove(root);
        this.root = root;
    }

    /**
     * Get previous Node instance from path
     */
    public Node prev(NodePath path) {
        NodeWithIndex nodeWithIndex = getWithIndex(path);

        if (nodeWithIndex.node.equals(root)) {
            return null;
        }

        int prevIndex = nodeWithIndex.index - 1;

        if (prevIndex < 0) {
            return null;
        }

        return ordered.get(prevIndex);
    }

    /**
     * Get next Node instance from path
     */
    public Node next(NodePath path) {
        NodeWithIndex nodeWithIndex = getWithIndex(path);

        if (nodeWithIndex.node.equals(root)) {
            return ordered.get(0);
        }

        int nextIndex = nodeWithIndex.index + 1;

        // next is out of range
        if (nextIndex > (ordered.size() - 1)) {
            return null;
        }

        return ordered.get(nextIndex);
    }

    /**
     * Get parent Node instance from path
     */
    public Node parent(NodePath path) {
        return getWithIndex(path).node.getParent();
    }

    public Node get(NodePath path) {
        return getWithIndex(path).node;
    }

    public String toYml() {
        return YmlParser.parse(this.root);
    }

    private NodeWithIndex getWithIndex(NodePath path) {
        NodeWithIndex nodeWithIndex = cached.get(path);

        if (Objects.isNull(nodeWithIndex)) {
            throw new IllegalArgumentException("The node path doesn't existed");
        }

        return nodeWithIndex;
    }

    /**
     * Reset node path and parent reference and put to cache
     */
    private void buildTree(Node root) {
        for (Node child : root.getChildren()) {
            child.setPath(NodePath.create(root.getPath(), child.getName()));
            child.setParent(root);
            buildTree(child);
        }

        ordered.add(root);
        cached.put(root.getPath(), new NodeWithIndex(root, ordered.size() - 1));
    }

    private class NodeWithIndex implements Serializable {

        Node node;

        int index;

        NodeWithIndex(Node node, int index) {
            this.node = node;
            this.index = index;
        }
    }
}
