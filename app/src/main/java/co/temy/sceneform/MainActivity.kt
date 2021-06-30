package co.temy.sceneform

import android.annotation.SuppressLint
import android.app.Activity
import android.app.ActivityManager
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.MotionEvent
import android.widget.Toast
import com.google.ar.core.Anchor
import com.google.ar.core.HitResult
import com.google.ar.core.Plane
import com.google.ar.sceneform.AnchorNode
import com.google.ar.sceneform.rendering.ModelRenderable
import com.google.ar.sceneform.ux.ArFragment
import com.google.ar.sceneform.ux.TransformableNode
import kotlinx.android.synthetic.main.activity_main.*
import java.util.concurrent.CompletableFuture

const val TAG = "CustomMaterialSample"
private const val MIN_OPENGL_VERSION = 3.0

class MainActivity : AppCompatActivity() {

    private lateinit var arFragment: ArFragment
    private lateinit var renderableModel: ModelRenderable

    private lateinit var renderableFuture: CompletableFuture<ModelRenderable>
    private lateinit var materialFutureBlue: CompletableFuture<CustomMaterial>
    private lateinit var materialFutureBlack: CompletableFuture<CustomMaterial>
    private lateinit var materialFutureWhite: CompletableFuture<CustomMaterial>

    private lateinit var customMaterial: CustomMaterial

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if (!checkIsSupportedDeviceOrFinish(this)) return

        arFragment = arFragmentView as ArFragment

        materialFutureBlue = CustomMaterial.build(this) {
            baseColorSource = Uri.parse("texture_blue/diffuse.png")
            metallicSource = Uri.parse("texture_blue/roughness.png")
            roughnessSource = Uri.parse("texture_blue/roughness.png")
            normalSource = Uri.parse("texture_blue/normal.png")
        }

        renderableFuture = ModelRenderable.builder()
            .setSource(this, R.raw.test3)
            .build()

        renderableFuture.thenAcceptBoth(materialFutureBlue) { renderableResult, materialResult ->
            Log.i(TAG, "init material and renderable")
            customMaterial = materialResult
            renderableModel = renderableResult
            renderableModel.material = customMaterial.value
        }

        buttonBlue.setOnClickListener {
            customMaterial.switchBaseColor()
            if (customMaterial.isDefaultBaseColorMap) {
                buttonBlue.setText(R.string.set_color)
            } else
                buttonBlue.setText(R.string.reset_color)
            customMaterial.switchMetallic()
            customMaterial.switchRoughness()
            customMaterial.switchNormal()
        }


        arFragment.setOnTapArPlaneListener { hitResult: HitResult, _: Plane, _: MotionEvent ->
            val anchor: Anchor = hitResult.createAnchor()
            val anchorNode = AnchorNode(anchor)
            anchorNode.setParent(arFragment.arSceneView.scene)

            val modelNode = TransformableNode(arFragment.transformationSystem)
            modelNode.setParent(anchorNode)
            modelNode.renderable = renderableModel
            modelNode.select()
        }
        buttonBlue.text="Blue"
    }

    @SuppressLint("ObsoleteSdkInt")
    private fun checkIsSupportedDeviceOrFinish(activity: Activity): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            Log.e(TAG, "Sceneform requires Android N or later")
            Toast.makeText(this, "Sceneform requires Android N or later", Toast.LENGTH_LONG).show()
            activity.finish()
            return false
        }
        val openGlVersionString: String = (activity.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager)
            .deviceConfigurationInfo
            .glEsVersion
        if (openGlVersionString.toDouble() < MIN_OPENGL_VERSION) {
            Log.e(TAG, "Sceneform requires OpenGL ES 3.0 or later")
            Toast.makeText(activity, "Sceneform requires OpenGL ES 3.0 or later", Toast.LENGTH_LONG).show()
            activity.finish()
            return false
        }
        return true
    }
}
