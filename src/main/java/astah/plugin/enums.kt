package astah.plugin

enum class Column(val index : Int, val text : String, val defaultWidth : Int, val att : (Ticket) -> Any) {
    TICKET_NUMBER(0, "#", 30, Ticket::issueId),
    TICKET_SUBJECT(1, "Ticket Subject", 180, Ticket::subject),
    TICKET_TRACKER(2, "Tracker", 80, Ticket::trackerName),
    TICKET_STATUS(3, "Status", 80, Ticket::statusName),
    TICKET_PRIORITY(4, "Priority", 30, Ticket::priority),
    TICKET_ASIGNEE(5, "Asignee", 30, Ticket::assigneeName),
    TICKET_START_DATE(6, "Start Date", 30, Ticket::startDate),
    TICKET_DUE_DATE(7, "Due Date", 30, Ticket::dueDate),
    DIAGRAM_NAME(8, "Diagram Name", 350, Ticket::diagramName),
    DIAGRAM_NAMESPACE(9, "Namespace", 100, Ticket::namespace),
    TICKET_DONE_RATIO(10, "Done Ratio(%)", 10, Ticket::doneRatio),
    TICKET_CATEGORY(11, "Category", 10, Ticket::categoryName),
    TICKET_ESTIMATED_HUORS(12, "Estimated Hours", 10, Ticket::estimatedHours),
    TICKET_TARGET_VERSION(13, "Target Version", 10, Ticket::targetVersionName),
    DIAGRAM_ID(14, "...", 5, Ticket::diagramId)
}
enum class AstahTag(val key : String) {
    REDMINE_TICKETS("astah.rtc.plugin.redmine.tickets"),
    REDMINE_URI("astah.rtc.plugin.redmine.uri"),
    REDMINE_KEY("astah.rtc.plugin.redmine.key"),
    REDMINE_PROJECT_ID("astah.rtc.plugin.redmine.projectid"),
    REDMINE_TABLE_COLUMN_WIDTHS("astah.rtc.plugin.redmine.widths")
}