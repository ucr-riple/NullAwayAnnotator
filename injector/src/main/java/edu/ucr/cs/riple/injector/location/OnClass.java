/*
 * MIT License
 *
 * Copyright (c) 2023 Nima Karimipour
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package edu.ucr.cs.riple.injector.location;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.BodyDeclaration;
import com.github.javaparser.ast.nodeTypes.NodeWithAnnotations;
import edu.ucr.cs.riple.injector.Helper;
import edu.ucr.cs.riple.injector.changes.Change;
import edu.ucr.cs.riple.injector.modifications.Modification;
import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;
import org.json.simple.JSONObject;

/** Represents a location for class element. This location is used to apply changes to a class. */
public class OnClass extends Location {

  /**
   * Pattern to detect if a class flat name is for an anonymous class. Anonymous classes flat names
   * ends with a $ and one or more digits.
   */
  public static final Pattern anonymousClassPattern = Pattern.compile(".*\\$\\d+$");

  public OnClass(Path path, String clazz) {
    super(LocationKind.CLASS, path, clazz);
  }

  public OnClass(String path, String clazz) {
    this(Helper.deserializePath(path), clazz);
  }

  @Override
  protected Modification applyToMember(NodeList<BodyDeclaration<?>> members, Change change) {
    if (isAnonymousClassFlatName(change.location.clazz)) {
      return null;
    }
    final AtomicReference<Modification> ans = new AtomicReference<>();
    Optional<Node> clazz = members.getParentNode();
    clazz.ifPresent(
        node ->
            node.getRange()
                .ifPresent(range -> ans.set(change.visit((NodeWithAnnotations<?>) node, range))));
    return ans.get();
  }

  /**
   * Checks if flat name is for an anonymous class.
   *
   * @return true, if flat name is for an anonymous class.
   */
  public static boolean isAnonymousClassFlatName(String flatName) {
    return anonymousClassPattern.matcher(flatName).matches();
  }

  @Override
  protected void fillJsonInformation(JSONObject res) {
    // no op
  }

  @Override
  public String toString() {
    return "OnClass{" + "type=" + type + ", clazz='" + clazz + '\'' + ", path=" + path + '}';
  }
}
