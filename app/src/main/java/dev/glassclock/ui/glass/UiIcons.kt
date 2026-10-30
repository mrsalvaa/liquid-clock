package dev.glassclock.ui.glass

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.automirrored.outlined.DriveFileMove
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.automirrored.rounded.DriveFileMove
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Alarm
import androidx.compose.material.icons.outlined.AlarmOff
import androidx.compose.material.icons.outlined.AlarmOn
import androidx.compose.material.icons.outlined.BookmarkAdd
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.CreateNewFolder
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.FolderOff
import androidx.compose.material.icons.outlined.HourglassBottom
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.SkipNext
import androidx.compose.material.icons.outlined.Snooze
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Alarm
import androidx.compose.material.icons.rounded.AlarmOff
import androidx.compose.material.icons.rounded.AlarmOn
import androidx.compose.material.icons.rounded.BookmarkAdd
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.CreateNewFolder
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.FolderOff
import androidx.compose.material.icons.rounded.HourglassBottom
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.Snooze
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Los íconos de la app se escriben con su versión Rounded (la de Liquid Glass); en el estilo
 * sólido se dibuja la versión Outlined, de líneas finas. Un ícono nuevo hay que sumarlo acá.
 */
private val SOLID_ICONS: Map<ImageVector, ImageVector> by lazy {
    mapOf(
        Icons.Rounded.Add to Icons.Outlined.Add,
        Icons.Rounded.Alarm to Icons.Outlined.Alarm,
        Icons.Rounded.AlarmOff to Icons.Outlined.AlarmOff,
        Icons.Rounded.AlarmOn to Icons.Outlined.AlarmOn,
        Icons.Rounded.BookmarkAdd to Icons.Outlined.BookmarkAdd,
        Icons.Rounded.Check to Icons.Outlined.Check,
        Icons.Rounded.Close to Icons.Outlined.Close,
        Icons.Rounded.ContentCopy to Icons.Outlined.ContentCopy,
        Icons.Rounded.CreateNewFolder to Icons.Outlined.CreateNewFolder,
        Icons.Rounded.Delete to Icons.Outlined.Delete,
        Icons.Rounded.Edit to Icons.Outlined.Edit,
        Icons.Rounded.ExpandMore to Icons.Outlined.ExpandMore,
        Icons.Rounded.Flag to Icons.Outlined.Flag,
        Icons.Rounded.Folder to Icons.Outlined.Folder,
        Icons.Rounded.FolderOff to Icons.Outlined.FolderOff,
        Icons.Rounded.HourglassBottom to Icons.Outlined.HourglassBottom,
        Icons.Rounded.Pause to Icons.Outlined.Pause,
        Icons.Rounded.PlayArrow to Icons.Outlined.PlayArrow,
        Icons.Rounded.Public to Icons.Outlined.Public,
        Icons.Rounded.Refresh to Icons.Outlined.Refresh,
        Icons.Rounded.Remove to Icons.Outlined.Remove,
        Icons.Rounded.Settings to Icons.Outlined.Settings,
        Icons.Rounded.SkipNext to Icons.Outlined.SkipNext,
        Icons.Rounded.Snooze to Icons.Outlined.Snooze,
        Icons.Rounded.Timer to Icons.Outlined.Timer,
        Icons.AutoMirrored.Rounded.ArrowBack to Icons.AutoMirrored.Outlined.ArrowBack,
        Icons.AutoMirrored.Rounded.ArrowForward to Icons.AutoMirrored.Outlined.ArrowForward,
        Icons.AutoMirrored.Rounded.DriveFileMove to Icons.AutoMirrored.Outlined.DriveFileMove,
    )
}

/** El ícono que corresponde al estilo actual. */
@Composable
fun styled(icon: ImageVector): ImageVector =
    if (LocalGlass.current.solid) SOLID_ICONS[icon] ?: icon else icon
