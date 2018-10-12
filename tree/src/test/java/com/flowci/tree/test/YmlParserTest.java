/*
 * Copyright 2018 flow.ci
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

package com.flowci.tree.test;

import com.flowci.tree.Node;
import com.flowci.tree.NodePath;
import com.flowci.tree.NodeTree;
import com.flowci.tree.YmlParser;
import com.google.common.io.Files;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.List;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * @author yang
 */
public class YmlParserTest {

    private String content;

    @Before
    public void init() throws IOException {
        ClassLoader classLoader = YmlParserTest.class.getClassLoader();
        URL resource = classLoader.getResource("flow.yml");
        content = Files.toString(new File(resource.getFile()), Charset.forName("UTF-8"));
    }

    @Test
    public void should_get_node_from_yml() {
        Node root = YmlParser.load("root", content);

        // verify flow
        Assert.assertEquals("root", root.getName());
        Assert.assertEquals("echo hello", root.getEnv("FLOW_WORKSPACE"));
        Assert.assertEquals("echo version", root.getEnv("FLOW_VERSION"));

        Assert.assertTrue(root.getSelector().getTags().contains("ios"));
        Assert.assertTrue(root.getSelector().getTags().contains("local"));

        Assert.assertEquals(3, root.getFilter().getBranches().size());
        Assert.assertEquals(1, root.getFilter().getTags().size());

        // verify steps
        List<Node> steps = root.getChildren();
        Assert.assertEquals(2, steps.size());

        Node step1 = steps.get(0);
        Assert.assertEquals("step-1", step1.getName()); // step-1 is default name
        Assert.assertEquals("echo step", step1.getEnv("FLOW_WORKSPACE"));
        Assert.assertEquals("echo step version", step1.getEnv("FLOW_VERSION"));

        Assert.assertTrue(step1.isAllowFailure());
        Assert.assertFalse(step1.isFinal());
        Assert.assertEquals("println(FLOW_WORKSPACE)\ntrue\n", step1.getBefore());

        Node step11 = step1.getChildren().get(0);
        Assert.assertNotNull(step11);
        Assert.assertEquals("step11", step11.getName());
        Assert.assertEquals("echo 1", step11.getScript());

        Node step12 = step1.getChildren().get(1);
        Assert.assertNotNull(step12);
        Assert.assertEquals("step12", step12.getName());
        Assert.assertEquals("echo 2", step12.getScript());

        Node step2 = steps.get(1);
        Assert.assertEquals("step2", step2.getName());
        Assert.assertEquals("true\n", step2.getAfter());
    }

    @Test
    public void should_get_correct_relationship_on_node_tree() {
        Node root = YmlParser.load("hello", content);
        NodeTree tree = NodeTree.create(root);
        Assert.assertEquals(root, tree.getRoot());

        // verify parent / child relationship
        Node step1 = tree.get(NodePath.create("root/step-1")); // step-1 is default name
        Assert.assertNotNull(step1);
        Assert.assertEquals(2, step1.getChildren().size());
        Assert.assertEquals(root, step1.getParent());

        Node step11 = tree.get(NodePath.create("root/step-1/step11"));
        Assert.assertNotNull(step11);
        Assert.assertTrue(step11.getChildren().isEmpty());
        Assert.assertEquals(step1, step11.getParent());

        Node step12 = tree.get(NodePath.create("root/step-1/step12"));
        Assert.assertNotNull(step12);
        Assert.assertTrue(step12.getChildren().isEmpty());
        Assert.assertEquals(step1, step12.getParent());

        Node step2 = tree.get(NodePath.create("root/step2"));
        Assert.assertNotNull(step2);
        Assert.assertTrue(step2.getChildren().isEmpty());
        Assert.assertEquals(root, step2.getParent());

        // verify next / previous relationship
        Assert.assertEquals(step11, tree.next(root.getPath()));
        Assert.assertEquals(step12, tree.next(step11.getPath()));
        Assert.assertEquals(step1, tree.next(step12.getPath()));
        Assert.assertEquals(step2, tree.next(step1.getPath()));
        Assert.assertNull(tree.next(step2.getPath()));
    }

    @Test
    public void should_parse_to_yml_from_node() {
        Node root = YmlParser.load("default", content);
        String parsed = YmlParser.parse(root);
        Assert.assertNotNull(parsed);
    }
}
