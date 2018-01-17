package astah.plugin

import com.taskadapter.redmineapi.bean.Issue
import java.text.SimpleDateFormat
import javax.swing.JOptionPane

object TicketsManager {
    enum class RedmineTag(val text : String) {
        ASTAH_TICKET_ID("#astahTicketId = "),
        ASTAH_PROJECT_NAME("#astahProjectname = "),
        ASTAH_DIAGRAM_NAME("#diagramName = "),
        ASTAH_NAMESPACE("#namespace = "),
        ASTAH_DIAGRAM_ID("#diagramId = ")
    }
    val dateFormat = SimpleDateFormat("yyyy/MM/dd")
    var tickets: MutableList<Ticket> = mutableListOf()

    fun setupRedmine(url : String, key : String, rp : String, ap : String) : Boolean {
        return RedmineAccessor.setupRedmine(url,key,rp,ap)
    }
    fun addTicket(ticket: Ticket) : Boolean {
        if (!RedmineAccessor.createTicket(ticket.astahTicketId,
                ticket.astahProjectId, ticket.diagramName, ticket.namespace, ticket.diagramId)) {
            JOptionPane.showMessageDialog(null,
                    "Creating a ticket in the redmine has been failed.")
            return false
        }
        tickets.add(0,ticket)
        if (!loadFromRedmine()) {
            return false
        }
        val newTicket = tickets.filter { it.astahTicketId == ticket.astahTicketId }.singleOrNull()
        if (newTicket != null) {
            RedmineAccessor.openTicketInBrowser(newTicket.issueId.toString())
        } else
            JOptionPane.showMessageDialog(null, "New ticket cannot be found")
        saveTicketsToProject()
        return true
    }
    private fun saveTicketsToProject() {
        with (AstahAccessor) {
            writeTaggedValue(AstahTag.REDMINE_TICKETS.key,
                    JsonSaveDataConverter.convertFromTicketsToJSON(tickets.toTypedArray()))
            writeTaggedValue(AstahTag.REDMINE_URI.key, RedmineAccessor.redmineUri)
            writeTaggedValue(AstahTag.REDMINE_KEY.key, RedmineAccessor.redmineKey)
            writeTaggedValue(AstahTag.REDMINE_PROJECT_ID.key, RedmineAccessor.redminePrj)
        }
    }
    fun closeAllTickets() {
        tickets.clear()
    }
    fun loadFromProject() : Boolean {
        val savedjson = AstahAccessor.readTaggedValue(AstahTag.REDMINE_TICKETS.key)
        val saveduri = AstahAccessor.readTaggedValue(AstahTag.REDMINE_URI.key)
        val savedkey = AstahAccessor.readTaggedValue(AstahTag.REDMINE_KEY.key)
        val savedproject = AstahAccessor.readTaggedValue(AstahTag.REDMINE_PROJECT_ID.key)
        val message = "The astah project file does not have redmine information or the information is incorrect."
        if (savedjson == null || saveduri == null || savedkey == null || savedproject == null) {
            if (!AstahAccessor.isNewlyCreatedProject())
                JOptionPane.showMessageDialog(null, message)
            return false
        }
        if (RedmineAccessor.setupRedmine(saveduri, savedkey, savedproject, AstahAccessor.getCurrentProjectName())) {
            val savedTickets = JsonSaveDataConverter.convertFromJsonToTickets(savedjson)
            tickets.clear()
            tickets.addAll(savedTickets)
            return true
        } else {
            JOptionPane.showMessageDialog(null,
                    "The redmine described in the astah project file is not responding.")
            return false
        }
    }
    fun updateFromProject() {
        tickets.forEach { ticket ->
            val diagram = AstahAccessor.findDiagram(ticket.diagramId)
            if (diagram != null) {
                ticket.diagramName = diagram.name
                ticket.namespace = AstahAccessor.getNamespace(diagram)
            } else {
                JOptionPane.showMessageDialog(null,
                        "The diagram ${ticket.diagramName} cannot be found in the current astah project.")
            }
        }
    }
    fun loadFromRedmine() : Boolean {
        val issues = RedmineAccessor.getIssues()
        val survivedTickets = mutableListOf<Ticket>()
        if (issues == null) {
            JOptionPane.showMessageDialog(null,
                    "Loading tickets from the redmine has failed.")
            return false
        } else {
            issues.forEach { issue ->
                val descriptionLines = issue.description.split("\r\n").toList()
                var astahTicketId : String? = null
                var astahProjcetName : String? = null
                var diagramName : String? = null
                var namespace : String? = null
                var diagramId : String? = null
                descriptionLines.forEach { line ->
                    if (line.startsWith(RedmineTag.ASTAH_TICKET_ID.text, true))
                        astahTicketId = line.drop(RedmineTag.ASTAH_TICKET_ID.text.length)
                    if (line.startsWith(RedmineTag.ASTAH_PROJECT_NAME.text, true))
                        astahProjcetName = line.drop(RedmineTag.ASTAH_PROJECT_NAME.text.length)
                    if (line.startsWith(RedmineTag.ASTAH_DIAGRAM_NAME.text, true))
                        diagramName = line.drop(RedmineTag.ASTAH_DIAGRAM_NAME.text.length)
                    if (line.startsWith(RedmineTag.ASTAH_NAMESPACE.text, true))
                        namespace = line.drop(RedmineTag.ASTAH_NAMESPACE.text.length)
                    if (line.startsWith(RedmineTag.ASTAH_DIAGRAM_ID.text, true))
                        diagramId = line.drop(RedmineTag.ASTAH_DIAGRAM_ID.text.length)
                }
                if (astahTicketId != null && astahProjcetName != null &&
                        diagramName != null && namespace != null && diagramId != null) {
                    var existingTicket : Ticket? = null
                    tickets.forEach { ticket ->
                        if (ticket.astahTicketId == astahTicketId) {
                            copyFromIssueToTicket(issue, ticket)
                            existingTicket = ticket
                            survivedTickets.add(ticket)
                        }
                    }
                    if (existingTicket == null && astahProjcetName == AstahAccessor.getCurrentProjectName()) {
                        val newTicket
                                = Ticket(astahTicketId!!,astahProjcetName!!,diagramName!!, namespace!!, diagramId!!)
                        copyFromIssueToTicket(issue, newTicket)
                        survivedTickets.add(newTicket)
                    }
                }
            }
        }
        tickets.clear()
        tickets.addAll(survivedTickets)
        saveTicketsToProject()
        return true
    }
    private fun copyFromIssueToTicket(issue : Issue, ticket : Ticket) {
        with (ticket) {
            subject = issue.subject
            issueId = issue.id
            trackerName = issue.tracker.name
            statusName = issue.statusName
            priority = Priority(issue.priorityId, issue.priorityText)
            assigneeName = issue.assignee?.fullName ?: ""
            startDate = if (issue.startDate == null) "" else dateFormat.format(issue.startDate)
            dueDate = if (issue.dueDate == null ) "" else dateFormat.format(issue.dueDate)
            doneRatio = issue.doneRatio
            categoryName = issue.category?.name ?: ""
            estimatedHours = issue.estimatedHours ?: 0F
            targetVersionName = issue.targetVersion?.name ?: ""
        }
    }
}