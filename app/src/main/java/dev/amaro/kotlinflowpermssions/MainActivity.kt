package dev.amaro.kotlinflowpermssions

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlin.random.Random

class MainActivity : AppCompatActivity() {

    private val manager = PermissionManager()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)


        manager.request(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
            .collectOnMain {
                val permission = it.permissions[0]
                val message =
                    "[${permission.javaClass.simpleName.toUpperCase()}] ${permission.permission}"
                Toast.makeText(this, message, Toast.LENGTH_LONG).show()
            }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        manager.handleResults(requestCode, permissions, grantResults)
    }


}


class PermissionManager {

    private val done = mutableListOf<Int>()
    private val requests = mutableMapOf<Int, PermissionRequest>()

    fun request(activity: AppCompatActivity, vararg permissions: String): Flow<PermissionRequest> {
        val code = Random(999).nextInt(5000)
        ActivityCompat.requestPermissions(activity, permissions, code)
        requests[code] = PermissionRequest(permissions.map { PermissionResult.Pending(it) })
        return flow {
            while (!done.contains(code)) {
                delay(200)
            }
            done.remove(code)
            this.emit(requests.remove(code)!!)
        }
    }

    fun handleResults(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        val request = requests.remove(requestCode)!!
        requests[requestCode] = request.copy(permissions = permissions.mapIndexed { i, p ->
            when (grantResults[i]) {
                PackageManager.PERMISSION_GRANTED -> PermissionResult.Granted(p)
                PackageManager.PERMISSION_DENIED -> PermissionResult.Denied(p)
                else -> PermissionResult.Pending(p)
            }
        })
        done.add(requestCode)
    }
}

data class PermissionRequest(val permissions: List<PermissionResult>)

sealed class PermissionResult(val permission: String) {
    class Granted(permission: String) : PermissionResult(permission)
    class Denied(permission: String) : PermissionResult(permission)
    class Pending(permission: String) : PermissionResult(permission)
}

fun <T> Flow<T>.collectOnMain(block: suspend (T) -> Unit) {
    GlobalScope.launch(Dispatchers.Main) {
        collect {
            block(it)
        }
    }
}
