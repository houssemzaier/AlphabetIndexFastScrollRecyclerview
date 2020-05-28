package fastscroll.app.alphabetindex

// import androidx.test.rule.ServiceTestRule

import android.graphics.RectF
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
//import androidx.test.rule.ActivityTestRule;



@RunWith(AndroidJUnit4::class)
class HelpersTest {
//    @get:Rule
//    val serviceRule = ServiceTestRule()

    @Test
    fun testExtension_RectF_contains() {
        val rectF = RectF(100F, 100F, 200F, 200F)
        Assert.assertFalse(rectF.contains(5F, 500F))
    }
}
