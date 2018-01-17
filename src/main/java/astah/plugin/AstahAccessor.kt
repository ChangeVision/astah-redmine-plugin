package astah.plugin

import com.change_vision.jude.api.inf.AstahAPI
import com.change_vision.jude.api.inf.editor.TransactionManager
import com.change_vision.jude.api.inf.model.IDiagram
import com.change_vision.jude.api.inf.model.ITaggedValue

object AstahAccessor {
    private val projectAccessor = AstahAPI.getAstahAPI().projectAccessor

    fun writeTaggedValue(tagKey : String , json: String) {
        try {
            TransactionManager.beginTransaction()
            val tag = getTaggedValue(tagKey, true)
            tag!!.value = json
            TransactionManager.endTransaction()
        } catch (e : Exception) {
            e.printStackTrace()
            if (TransactionManager.isInTransaction()) {
                TransactionManager.abortTransaction()
            }
        }
    }
    fun readTaggedValue(tagKey : String) : String? {
        try {
            val tag = getTaggedValue(tagKey, false)
            if (tag != null)
                return tag.getValue()
        } catch (e : Exception) {
                e.printStackTrace()
        }
        return null
    }
    private fun getTaggedValue(tagKey : String, isCreateIfNotExist : Boolean) : ITaggedValue? {
        var ret : ITaggedValue? = null
        val isInTransaction = TransactionManager.isInTransaction()
        try {
            val project = projectAccessor.project
            ret = project.taggedValues.filter { it.key.equals(tagKey,ignoreCase = true) }.firstOrNull()
            if (ret == null && isCreateIfNotExist) {
                if (!isInTransaction)
                    TransactionManager.beginTransaction()
                val bme = projectAccessor.modelEditorFactory.basicModelEditor
                ret = bme.createTaggedValue(project, tagKey, "")
                if (!isInTransaction)
                    TransactionManager.endTransaction()
            }
        } catch (exp : Exception) {
            exp.printStackTrace()
            if (!isInTransaction && TransactionManager.isInTransaction()) {
                TransactionManager.abortTransaction()
            }
        }
        return ret
    }
    fun getCurrentProjectName() : String {
        return projectAccessor.project.name
    }
    fun getCurrentDiagram() : IDiagram? {
        return projectAccessor.viewManager.diagramViewManager.currentDiagram
    }
    fun isNewlyCreatedProject() : Boolean {
        return projectAccessor.projectPath == "no_title"
    }
    fun getNamespace(d : IDiagram) : String {
        return d.getFullNamespace(".")
    }
    fun findDiagram(diagramId : String) : IDiagram? {
        return projectAccessor.findElements { e -> e.id == diagramId }.singleOrNull() as IDiagram
    }
    fun selectDiagram(diagramId : String) : Boolean {
        val d = findDiagram(diagramId)
        return d != null && selectDiagram(d)
    }
    private fun selectDiagram(d : IDiagram) : Boolean {
        try {
            projectAccessor.viewManager.diagramViewManager.open(d)
            return true
        } catch (e : Exception) {
            e.printStackTrace()
            return false
        }
    }
}