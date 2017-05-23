/* ************************************************************************************* */
/* *    PhatWare WritePad SDK                                                          * */
/* *    Copyright (c) 1997-2015 PhatWare(r) Corp. All rights reserved.                 * */
/* ************************************************************************************* */

/* ************************************************************************************* *
 *
 * WritePad Android Sample
 *
 * Unauthorized distribution of this code is prohibited. For more information
 * refer to the End User Software License Agreement provided with this 
 * software.
 *
 * This source code is distributed and supported by PhatWare Corp.
 * http://www.phatware.com
 *
 * THIS SAMPLE CODE CAN BE USED  AS A REFERENCE AND, IN ITS BINARY FORM, 
 * IN THE USER'S PROJECT WHICH IS INTEGRATED WITH THE WRITEPAD SDK. 
 * ANY OTHER USE OF THIS CODE IS PROHIBITED.
 * 
 * THE MATERIAL EMBODIED ON THIS SOFTWARE IS PROVIDED TO YOU "AS-IS"
 * AND WITHOUT WARRANTY OF ANY KIND, EXPRESS, IMPLIED OR OTHERWISE,
 * INCLUDING WITHOUT LIMITATION, ANY WARRANTY OF MERCHANTABILITY OR
 * FITNESS FOR A PARTICULAR PURPOSE. IN NO EVENT SHALL PHATWARE CORP.  
 * BE LIABLE TO YOU OR ANYONE ELSE FOR ANY DIRECT, SPECIAL, INCIDENTAL, 
 * INDIRECT OR CONSEQUENTIAL DAMAGES OF ANY KIND, OR ANY DAMAGES WHATSOEVER, 
 * INCLUDING WITHOUT LIMITATION, LOSS OF PROFIT, LOSS OF USE, SAVINGS 
 * OR REVENUE, OR THE CLAIMS OF THIRD PARTIES, WHETHER OR NOT PHATWARE CORP.
 * HAS BEEN ADVISED OF THE POSSIBILITY OF SUCH LOSS, HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, ARISING OUT OF OR IN CONNECTION WITH THE
 * POSSESSION, USE OR PERFORMANCE OF THIS SOFTWARE.
 * 
 * US Government Users Restricted Rights 
 * Use, duplication, or disclosure by the Government is subject to
 * restrictions set forth in EULA and in FAR 52.227.19(c)(2) or subparagraph
 * (c)(1)(ii) of the Rights in Technical Data and Computer Software
 * clause at DFARS 252.227-7013 and/or in similar or successor
 * clauses in the FAR or the DOD or NASA FAR Supplement.
 * Unpublished-- rights reserved under the copyright laws of the
 * United States.  Contractor/manufacturer is PhatWare Corp.
 * 10414 W. Highway 2, Ste 4-121 Spokane, WA 99224
 *
 * ************************************************************************************* */

package io_developers.sssemil.com.wand

import android.graphics.*
import android.os.Handler
import android.os.Message
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.widget.TextView
import com.phatware.android.RecoInterface.WritePadAPI
import com.phatware.android.WritePadManager
import java.util.*

class InkView @JvmOverloads constructor(context: android.content.Context, attrs: AttributeSet? = null, defStyle: Int = 0) : View(context, attrs, defStyle), OnInkViewListener {
    private val GRID_GAP = 65f
    var brushColor = Color.BLUE
    var brushWidth = 3
    var wordsss: String? = null
        private set
    private var textView: TextView? = null
    // Define the Handler that receives messages from the thread and update the progress
    private val mHandler = object : Handler() {
        override fun handleMessage(msg: Message) {
            textView!!.text = ""
            val words = WritePadManager.recoResultColumnCount()
            for (w in 0..words - 1) {
                val alternatives = WritePadManager.recoResultRowCount(w)
                if (alternatives > 0) {
                    val alternativesCollection = arrayOfNulls<CharSequence>(alternatives)
                    for (a in 0..alternatives - 1) {
                        val word = WritePadManager.recoResultWord(w, a)
                        alternativesCollection[a] = word
                    }

                    wordsss = WritePadManager.recoResultWord(w, 0)

                    textView!!.text = textView!!.text.toString() + " " + wordsss
                }
            }
        }

    }
    private var mStoredX = 0f
    private var mStoredY = 0f
    private var path: Path? = null
    private var mCurrStroke: Int = 0
    private var mPaint: Paint? = null
    private val mResultPaint: Paint
    private val mPathList: LinkedList<Path>
    private var mX: Float = 0.toFloat()
    private var mY: Float = 0.toFloat()
    private var mMoved: Boolean = false
    private val gridpath = Path()
    private val mLastTime: Long = 0
    private var mClickStart: Long = 0
    private val mLastWasTouchUp = false
    private var mPrevWasShow: Boolean = false

    init {

        path = Path()
        mPathList = LinkedList<Path>()
        mCurrStroke = -1
        mPaint = Paint()
        mPaint = Paint()
        mPaint!!.isAntiAlias = true
        mPaint!!.isDither = true
        mPaint!!.color = Color.WHITE
        mPaint!!.style = Paint.Style.STROKE
        mPaint!!.strokeJoin = Paint.Join.ROUND
        mPaint!!.strokeCap = Paint.Cap.ROUND
        mPaint!!.strokeWidth = 3f

        mResultPaint = Paint()
        mResultPaint.textSize = 32f
        mResultPaint.isAntiAlias = true
        mResultPaint.setARGB(0xff, 0x00, 0x00, 0x00)
    }

    override fun cleanView(emptyAll: Boolean) {
        WritePadManager.recoResetInk()
        mCurrStroke = -1
        mPathList.clear()
        path!!.reset()
        textView!!.text = ""

        mStoredX = 0f
        mStoredY = 0f

        invalidate()
    }

    fun getBitmap(): Bitmap {
        val b: Bitmap = Bitmap.createBitmap(width, height, Bitmap.Config
                .ARGB_8888)

        val canvas = Canvas(b)

        draw(canvas)

        return b
    }

    override fun getHandler(): Handler {
        return mHandler
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
    }

    override fun onDraw(canvas: Canvas) {
        mPaint!!.color = Color.WHITE
        mPaint!!.strokeWidth = 1f

        var y = GRID_GAP
        while (y < canvas.height) {
            gridpath.reset()
            gridpath.moveTo(0f, y)
            gridpath.lineTo(canvas.width.toFloat(), y)
            canvas.drawPath(gridpath, mPaint!!)
            y += GRID_GAP
        }
        mPaint!!.color = brushColor
        mPaint!!.strokeWidth = brushWidth.toFloat()


        for (aMPathList in mPathList) {
            canvas.drawPath(aMPathList, mPaint!!)
        }
        canvas.drawPath(path!!, mPaint!!)
    }

    private fun touch_start(x: Float, y: Float) {
        path!!.reset()
        path!!.moveTo(x, y)
        mX = x
        mY = y
        mMoved = false
        mCurrStroke = WritePadManager.recoNewStroke(3.0f, 0xFF0000FF.toInt())
        if (mCurrStroke >= 0) {
            val res = WritePadManager.recoAddPixel(mCurrStroke, x, y)
            if (res < 1) {
                // TODO: error
            }
        }
    }


    private fun touch_move(x: Float, y: Float) {
        val dx = Math.abs(x - mX)
        val dy = Math.abs(y - mY)
        if (dx >= TOUCH_TOLERANCE || dy >= TOUCH_TOLERANCE) {
            path!!.quadTo(mX, mY, (x + mX) / 2, (y + mY) / 2)
            mMoved = true
            mX = x
            mY = y
            if (mCurrStroke >= 0) {
                val res = WritePadManager.recoAddPixel(mCurrStroke, x, y)
                if (res < 1) {
                    // TODO: error
                }
            }
        }
    }

    fun touch_up() {
        // stopRecognizer();

        mCurrStroke = -1
        if (!mMoved)
            mX++
        mMoved = false
        path!!.lineTo(mX, mY)
        mPathList.add(path!!)
        path = Path()
        invalidate()

        val rt = context as MainActivity
        val nStrokeCnt = WritePadManager.recoStrokeCount()
        if (nStrokeCnt == 1) {
            var gesturetype = WritePadAPI.GEST_DELETE + WritePadAPI.GEST_RETURN + WritePadAPI.GEST_SPACE +
                    WritePadAPI.GEST_TAB + WritePadAPI.GEST_BACK + WritePadAPI.GEST_UNDO
            gesturetype = WritePadManager.recoGesture(gesturetype)
            if (gesturetype != WritePadAPI.GEST_NONE) {
                // TODO: process gesture
                WritePadManager.recoDeleteLastStroke()
                mPathList.removeLast()
                return
            }
        } else if (nStrokeCnt > 1) {
            var gesturetype = WritePadAPI.GEST_CUT + WritePadAPI.GEST_BACK + WritePadAPI.GEST_RETURN
            gesturetype = WritePadManager.recoGesture(gesturetype)
            if (gesturetype != WritePadAPI.GEST_NONE && gesturetype != WritePadAPI.GEST_BACK) {
                // TODO: process gesture
                WritePadManager.recoDeleteLastStroke()
                mPathList.removeLast()
                when (gesturetype) {
                // case WritePadAPI.GEST_BACK:
                    WritePadAPI.GEST_BACK_LONG -> {
                        WritePadManager.recoDeleteLastStroke()
                        mPathList.removeLast()
                        if (WritePadManager.recoStrokeCount() < 1) {
                            textView!!.text = ""
                        }

                        rt.boundService?.dataNotify(WritePadManager.recoStrokeCount())
                        return
                    }

                    WritePadAPI.GEST_CUT -> {
                        cleanView(false)
                        return
                    }

                    WritePadAPI.GEST_RETURN -> {
                        sendText()
                        cleanView(false)
                        return
                    }

                    else -> {
                    }
                }
            }
        }

        // notify recognizer thread about data availability
        rt.boundService?.dataNotify(nStrokeCnt)
    }

    private fun sendText() {


    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val x = event.x
        val y = event.y

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                touch_start(x, y)
                invalidate()
            }

            MotionEvent.ACTION_MOVE -> {
                var i = 0
                val n = event.historySize
                while (i < n) {
                    touch_move(event.getHistoricalX(i),
                            event.getHistoricalY(i))
                    i++
                }
                touch_move(x, y)
                invalidate()
            }

            MotionEvent.ACTION_UP -> {
                touch_up()
                invalidate()

                val currentTimeMillis = System.currentTimeMillis()
                if (currentTimeMillis - mClickStart < 200) {
                    this.cleanView(true)
                    invalidate()
                }
                mClickStart = currentTimeMillis
            }
        }
        return true
    }

    internal fun onWandEvent(xD: Float, yD: Float, show: Boolean) {
        mStoredX -= (xD / 10.0).toFloat()
        mStoredY -= (yD / 10.0).toFloat()

        var action: Int

        if (mPrevWasShow && !show) {
            action = MotionEvent.ACTION_UP
            Log.i("onWandEvent", "UP x: $mStoredX y: $mStoredY")
        } else if (mPrevWasShow) {
            action = MotionEvent.ACTION_MOVE
            Log.i("onWandEvent", "MOVE x: $mStoredX y: $mStoredY")
        } else if (show) {
            action = MotionEvent.ACTION_DOWN
            Log.i("onWandEvent", "DOWN x: $mStoredX y: $mStoredY")
        } else {
            Log.i("onWandEvent", "NULL x: $mStoredX y: $mStoredY")
            return
        }

        if (mStoredX > width) {
            mStoredX = 0f
            mStoredY += 100
            action = MotionEvent.ACTION_OUTSIDE
        }

        if (mStoredY > height) {
            mStoredY = 0f
            action = MotionEvent.ACTION_OUTSIDE
        }

        if (mStoredX < 0) {
            mStoredX = width.toFloat()
            mStoredY -= 100
            action = MotionEvent.ACTION_OUTSIDE
        }

        if (mStoredY < 0) {
            mStoredY = height.toFloat()
            action = MotionEvent.ACTION_OUTSIDE
        }

        when (action) {
            MotionEvent.ACTION_DOWN -> {
                touch_start(mStoredX, mStoredY)
                invalidate()
            }

            MotionEvent.ACTION_MOVE -> {
                touch_move(mStoredX, mStoredY)
                invalidate()
            }

            MotionEvent.ACTION_UP -> {
                touch_up()
                invalidate()
            }
            MotionEvent.ACTION_OUTSIDE -> {
                touch_up()
                touch_start(mStoredX, mStoredY)
                invalidate()
            }
        }

        mPrevWasShow = show
    }

    fun setRecognizedTextContainer(textView: TextView) {
        this.textView = textView
    }

    companion object {
        private val TOUCH_TOLERANCE = 2f
    }
}