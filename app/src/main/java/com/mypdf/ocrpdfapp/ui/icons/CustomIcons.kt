package com.mypdf.ocrpdfapp.ui.icons

import androidx.compose.material.icons.materialIcon
import androidx.compose.material.icons.materialPath
import androidx.compose.ui.graphics.vector.ImageVector

object CustomIcons {
    val Compress: ImageVector
        get() {
            if (_compress != null) {
                return _compress!!
            }
            _compress = materialIcon(name = "Compress") {
                materialPath {
                    // Simple compress icon path - two arrows pointing inward
                    moveTo(4.0f, 9.0f)
                    lineTo(9.0f, 4.0f)
                    lineTo(11.0f, 6.0f)
                    lineTo(11.0f, 1.0f)
                    lineTo(6.0f, 1.0f)
                    lineTo(8.0f, 3.0f)
                    lineTo(3.0f, 8.0f)
                    close()
                    
                    moveTo(20.0f, 15.0f)
                    lineTo(15.0f, 20.0f)
                    lineTo(13.0f, 18.0f)
                    lineTo(13.0f, 23.0f)
                    lineTo(18.0f, 23.0f)
                    lineTo(16.0f, 21.0f)
                    lineTo(21.0f, 16.0f)
                    close()
                    
                    // Box in the middle
                    moveTo(8.0f, 8.0f)
                    lineTo(16.0f, 8.0f)
                    lineTo(16.0f, 16.0f)
                    lineTo(8.0f, 16.0f)
                    close()
                }
            }
            return _compress!!
        }

    private var _compress: ImageVector? = null
} 