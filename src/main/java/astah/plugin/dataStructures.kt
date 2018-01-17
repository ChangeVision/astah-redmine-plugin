package astah.plugin

data class Priority(val id: Int, val text: String)
data class Ticket(val astahTicketId : String, val astahProjectId : String, var diagramName : String,
                  var namespace: String, val diagramId : String)  {
    var issueId: Int = -1
    var subject: String = ""
    var priority : Priority = Priority(0,"")
    var trackerName : String = ""
    var statusName : String = ""
    var assigneeName : String = ""
    var startDate : String = ""
    var dueDate : String = ""
    var doneRatio : Int = 0
    var categoryName : String = ""
    var estimatedHours : Float = 0F
    var targetVersionName : String = ""
}
data class ColumnWidth(val index : Int, val width : Int)