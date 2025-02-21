/*
 * MIT License
 *
 * Copyright (c) 2025 Nima Karimipour
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

package edu.ucr.cs.riple.core.util;

import java.io.File;
import java.io.IOException;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.ResetCommand;
import org.eclipse.jgit.api.Status;
import org.eclipse.jgit.api.TransportConfigCallback;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.transport.RefSpec;
import org.eclipse.jgit.transport.SshTransport;
import org.eclipse.jgit.transport.sshd.SshdSessionFactory;

/** Utility class for Git operations. */
public class GitUtility implements AutoCloseable {
  /** Git repository instance. */
  private final Git git;

  /** Transport configuration callback for SSH transport. */
  private final TransportConfigCallback transportConfigCallback;

  /**
   * Constructor for GitUtility.
   *
   * @param repoPath Path to the Git repository.
   */
  public GitUtility(String repoPath) {
    try {
      this.git = Git.open(new File(repoPath));
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    this.transportConfigCallback = createSshTransport();
  }

  /**
   * resets the Git repository to the last commit.
   *
   * @throws Exception if an error occurs during the reset operation.
   */
  public void resetHard() throws Exception {
    git.reset().setMode(ResetCommand.ResetType.HARD).call();
  }

  /**
   * pulls the latest changes from the remote repository.
   *
   * @throws Exception if an error occurs during the pull operation.
   */
  public void pull() throws Exception {
    git.pull().setTransportConfigCallback(transportConfigCallback).call();
  }

  /**
   * checks out a specific branch in the Git repository.
   *
   * @param branchName the name of the branch to check out.
   *     <p>throws Exception if an error occurs during the checkout operation.
   */
  public void checkoutBranch(String branchName) throws Exception {
    git.checkout().setName(branchName).call();
  }

  /**
   * Deletes a local branch in the Git repository.
   *
   * @param branchName the name of the branch to delete.
   * @throws Exception if an error occurs during the delete operation.
   */
  public void deleteLocalBranch(String branchName) throws Exception {
    git.branchDelete().setBranchNames(branchName).setForce(true).call();
  }

  /**
   * Deletes a remote branch in the Git repository.
   *
   * @param branchName the name of the branch to delete.
   * @throws Exception if an error occurs during the delete operation.
   */
  public void deleteRemoteBranch(String branchName) throws Exception {
    git.push()
        .setRefSpecs(new RefSpec(":refs/heads/" + branchName))
        .setTransportConfigCallback(transportConfigCallback)
        .call();
  }

  /**
   * Creates and checks out a new branch in the Git repository.
   *
   * @param branchName the name of the new branch to create and check out.
   * @throws Exception if an error occurs during the create and checkout operation.
   */
  public void createAndCheckoutBranch(String branchName) throws Exception {
    git.checkout().setCreateBranch(true).setName(branchName).call();
  }

  /**
   * Pushes a branch to the remote repository.
   *
   * @param branchName the name of the branch to push.
   * @throws Exception if an error occurs during the push operation.
   */
  public void pushBranch(String branchName) throws Exception {
    git.push()
        .setRemote("origin")
        .setRefSpecs(new RefSpec(branchName))
        .setTransportConfigCallback(transportConfigCallback)
        .call();
  }

  /**
   * Checks if there are changes to commit in the Git repository.
   *
   * @return true if there are changes to commit, false otherwise.
   * @throws Exception if an error occurs during the status check operation.
   */
  public boolean hasChangesToCommit() throws Exception {
    Status status = git.status().call();
    return !status.isClean();
  }

  /**
   * Stages all changes in the Git repository.
   *
   * @throws Exception if an error occurs during the stage operation.
   */
  public void stageAllChanges() throws Exception {
    git.add().addFilepattern(".").call();
  }

  /**
   * Commits the changes in the Git repository with a specified commit message.
   *
   * @param message the commit message.
   * @throws Exception if an error occurs during the commit operation.
   */
  public void commitChanges(String message) throws Exception {
    git.commit().setMessage(message).call();
  }

  /**
   * Pushes the changes to the remote repository.
   *
   * @throws Exception if an error occurs during the push operation.
   */
  public void pushChanges() throws Exception {
    git.push().setTransportConfigCallback(transportConfigCallback).call();
  }

  /**
   * Reverts the last commit in the Git repository.
   *
   * @throws Exception if an error occurs during the revert operation.
   */
  public void revertLastCommit() throws Exception {
    RevCommit head = git.getRepository().parseCommit(git.getRepository().resolve("HEAD"));
    git.revert().include(head).call();
    pushChanges();
  }

  /** Closes the Git repository. */
  @Override
  public void close() {
    git.close();
  }

  /**
   * Creates a transport config callback for SSH transport that uses ssh session factory.
   *
   * @return the transport config callback for SSH transport.
   */
  private TransportConfigCallback createSshTransport() {
    return transport -> {
      if (transport instanceof SshTransport) {
        ((SshTransport) transport).setSshSessionFactory(SshdSessionFactory.getInstance());
      }
    };
  }
}
