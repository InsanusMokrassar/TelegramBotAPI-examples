import dev.inmo.micro_utils.common.MPPFile
import java.io.File

/** JVM picker backed by [File], returning a file root directly or descending through random directory children. */
actual fun pickFile(currentRoot: MPPFile): File? {
    if (currentRoot.isFile) {
        return currentRoot
    } else {
        return pickFile(currentRoot.listFiles() ?.takeIf { it.isNotEmpty() } ?.random() ?: return null)
    }
}
