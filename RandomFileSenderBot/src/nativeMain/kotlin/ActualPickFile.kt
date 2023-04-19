import dev.inmo.micro_utils.common.MPPFile
import okio.FileSystem

actual fun pickFile(currentRoot: MPPFile): MPPFile? {
    if (FileSystem.SYSTEM.exists(currentRoot) && FileSystem.SYSTEM.listOrNull(currentRoot) == null) {
        return currentRoot
    } else {
        return pickFile(FileSystem.SYSTEM.list(currentRoot).takeIf { it.isNotEmpty() } ?.random() ?: return null)
    }
}
