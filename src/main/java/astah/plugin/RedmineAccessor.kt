package astah.plugin

import com.taskadapter.redmineapi.IssueManager
import com.taskadapter.redmineapi.RedmineException
import com.taskadapter.redmineapi.RedmineManager
import com.taskadapter.redmineapi.RedmineManagerFactory
import com.taskadapter.redmineapi.bean.Issue
import com.taskadapter.redmineapi.bean.IssueFactory
import java.net.URI

object RedmineAccessor {
    var redmineUri : String = ""
    var redmineKey : String = ""
    var redminePrj : String = ""
    private var astahProjectName : String = ""
    private var redmineManager : RedmineManager? = null
    private var issueManager : IssueManager? = null

    fun setupRedmine(uri : String, key : String, rp : String, ap : String) : Boolean {
        try {
            redmineManager = RedmineManagerFactory.createWithApiKey(uri, key)
            issueManager = redmineManager!!.issueManager
            redmineUri = uri
            redmineKey = key
            redminePrj = rp
            astahProjectName = ap
            return true
        } catch (_ : Exception) {
            return false
        }
    }
    fun createTicket(astahTicketId : String, astahProjectName : String, diagramName : String,
                     namespace : String, diagramId : String) : Boolean {
        if (issueManager!= null && redminePrj != "") {
            try {
                val issue = IssueFactory.create(redmineManager!!.projectManager.getProjectByKey(redminePrj).id,
                        "(タイトルを編集してください)")
                issue.description = "#astahTicketId = "+astahTicketId+System.getProperty("line.separator")+
                        "#astahProjectName = "+astahProjectName+System.getProperty("line.separator")+
                        "#diagramName = "+diagramName+System.getProperty("line.separator")+
                        "#namespace = "+namespace+System.getProperty("line.separator")+
                        "#diagramId = "+diagramId
                issueManager!!.createIssue(issue)
                return true
            } catch (_ : RedmineException) {
                return false
            }
        } else {
            return false
        }
    }
    fun getIssues() : List<Issue>? {
        if (issueManager != null) {
            try {
                return issueManager!!.getIssues(redminePrj, null).toList()
            } catch (_ : RedmineException) {
                return null
            }
        } else {
            return null
        }
    }
    fun openTicketInBrowser(issueNumber : String) {
        java.awt.Desktop.getDesktop().browse(URI(redmineUri + "/issues/" + issueNumber))
    }
}