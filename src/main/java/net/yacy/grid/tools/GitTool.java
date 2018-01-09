/**
 *  GitTool
 *  (C) 2018 by Michael Peter Christen; mc@yacy.net, Frankfurt a. M., Germany
 *  first published 09.01.2018 on http://yacy.net
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 2.1 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public License
 *  along with this program in the file lgpl21.txt
 *  If not, see <http://www.gnu.org/licenses/>.
 */

package net.yacy.grid.tools;

import java.io.File;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;

public class GitTool {

    public String name, message, branch;
    
    public GitTool() {
        File gitWorkDir = new File(".");
        try {
            Git git = Git.open(gitWorkDir);
            Iterable<RevCommit> commits = git.log().all().call();
            Repository repo = git.getRepository();
            branch = repo.getBranch();
            RevCommit latestCommit = commits.iterator().next();
            name = latestCommit.getName();
            message = latestCommit.getFullMessage();
        } catch (Throwable e) {
            name = "";
            message = "";
            branch = "";
        }
    }
    
    public String toString() {
         return "branch " + branch + ": commit " + name + " " + message;
    }
    
    public static void main(String[] args) {
        System.out.println(new GitTool().toString());     
    }

}
