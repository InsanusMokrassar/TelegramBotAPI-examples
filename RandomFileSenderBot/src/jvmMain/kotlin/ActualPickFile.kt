import dev.inmo.micro_utils.common.MPPFile
import java.io.File

actual fun pickFile(currentRoot: MPPFile): File? {
    if (currentRoot.isFile) {
        return currentRoot
    } else {
        return pickFile(currentRoot.listFiles() ?.takeIf { it.isNotEmpty() } ?.random() ?: return null)
    }
}
