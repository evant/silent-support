/*
 * Copyright (C) 2011 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package me.tatarka.silentsupport.lint;

import com.google.common.annotations.Beta;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Useful utility methods related to lint.
 * <p>
 * <b>NOTE: This is not a public or final API; if you rely on this be prepared
 * to adjust your code for the next tools release.</b>
 */
@Beta
public class LintUtils {
    public static final String DOT_XML = ".xml";

    /**
     * Format a list of strings, and cut of the list at {@code maxItems} if the
     * number of items are greater.
     *
     * @param strings  the list of strings to print out as a comma separated list
     * @param maxItems the maximum number of items to print
     * @return a comma separated list
     */
    public static String formatList(List<String> strings, int maxItems) {
        StringBuilder sb = new StringBuilder(20 * strings.size());
        for (int i = 0, n = strings.size(); i < n; i++) {
            if (sb.length() > 0) {
                sb.append(", "); //$NON-NLS-1$
            }
            sb.append(strings.get(i));
            if (i == maxItems - 1 && n > maxItems) {
                sb.append(String.format("... (%1$d more)", n - i - 1));
                break;
            }
        }
        return sb.toString();
    }

    /**
     * Returns true if the given file represents an XML file
     *
     * @param file the file to be checked
     * @return true if the given file is an xml file
     */
    public static boolean isXmlFile(File file) {
        String string = file.getName();
        return string.regionMatches(true, string.length() - DOT_XML.length(),
                DOT_XML, 0, DOT_XML.length());
    }

    /**
     * Case insensitive ends with
     *
     * @param string the string to be tested whether it ends with the given
     *               suffix
     * @param suffix the suffix to check
     * @return true if {@code string} ends with {@code suffix},
     * case-insensitively.
     */
    public static boolean endsWith(String string, String suffix) {
        return string.regionMatches(true /* ignoreCase */, string.length() - suffix.length(),
                suffix, 0, suffix.length());
    }

    /**
     * Returns the children elements of the given node
     *
     * @param node the parent node
     * @return a list of element children, never null
     */
    public static List<Element> getChildren(Node node) {
        NodeList childNodes = node.getChildNodes();
        List<Element> children = new ArrayList<Element>(childNodes.getLength());
        for (int i = 0, n = childNodes.getLength(); i < n; i++) {
            Node child = childNodes.item(i);
            if (child.getNodeType() == Node.ELEMENT_NODE) {
                children.add((Element) child);
            }
        }
        return children;
    }

    /**
     * Returns the <b>number</b> of children of the given node
     *
     * @param node the parent node
     * @return the count of element children
     */
    public static int getChildCount(Node node) {
        NodeList childNodes = node.getChildNodes();
        int childCount = 0;
        for (int i = 0, n = childNodes.getLength(); i < n; i++) {
            Node child = childNodes.item(i);
            if (child.getNodeType() == Node.ELEMENT_NODE) {
                childCount++;
            }
        }
        return childCount;
    }
}