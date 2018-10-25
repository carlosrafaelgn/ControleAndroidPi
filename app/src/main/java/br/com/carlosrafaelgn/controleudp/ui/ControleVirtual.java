//
// MIT License
//
// Copyright (c) 2018 Carlos Rafael Gimenes das Neves
//
// Permission is hereby granted, free of charge, to any person obtaining a copy
// of this software and associated documentation files (the "Software"), to deal
// in the Software without restriction, including without limitation the rights
// to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
// copies of the Software, and to permit persons to whom the Software is
// furnished to do so, subject to the following conditions:
//
// The above copyright notice and this permission notice shall be included in all
// copies or substantial portions of the Software.
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
// IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
// FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
// AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
// LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
// OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
// SOFTWARE.
//
// https://github.com/carlosrafaelgn/ControleAndroidPi
//

package br.com.carlosrafaelgn.controleudp.ui;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.os.Build;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewDebug;

import br.com.carlosrafaelgn.controleudp.R;

@SuppressWarnings("unused")
public class ControleVirtual extends View {
	public interface Listener {
		void estadoTeclasAlterado(ControleVirtual controleVirtual);
		void teclaConfiguracao(ControleVirtual controleVirtual);
	}

	public static final int QUANTIDADE_TECLAS = 19;
	private static final int QUANTIDADE_TECLAS_NA_TELA = QUANTIDADE_TECLAS + 3;
	private static final int TAMANHO_MINIMO = 100;

	private static final int TECLA_W = 0;
	private static final int TECLA_A = 1;
	private static final int TECLA_S = 2;
	private static final int TECLA_D = 3;
	private static final int TECLA_E = 4;
	private static final int TECLA_SHIFT = 5;
	private static final int TECLA_ESPACO = 6;
	private static final int TECLA_ENTER = 7;
	private static final int TECLA_ESC = 8;
	private static final int TECLA_MOUSE_ESQ = 9;
	private static final int TECLA_MOUSE_DIR = 10;
	private static final int TECLA_1 = 11;
	private static final int TECLA_2 = 12;
	private static final int TECLA_3 = 13;
	private static final int TECLA_4 = 14;
	private static final int TECLA_5 = 15;
	private static final int TECLA_6 = 16;
	private static final int TECLA_7 = 17;
	private static final int TECLA_8 = 18;
	private static final int TECLA_WA = 19;
	private static final int TECLA_WD = 20;
	private static final int TECLA_CFG = 21;

	private static final String[] TEXTO_TECLAS = new String[] {
		"W", "A", "S", "D",
		"E", "⇧", " ", "↳", "Esc",
		"↰", "↱",
		"1", "2", "3", "4", "5", "6", "7", "8",
		"W+A", "W+D",
		"Cfg"
	};

	private Listener listener;
	private int estadoTeclas;
	private Rect[] rectTeclas;
	private int yTexto, colorPrimary, colorPrimaryDark, colorAccent, textoPrimary, textoAccent, teclaCfg;
	private Paint paint;
	private TextPaint textPaint;

	private static final int QUANTIDADE_MAXIMA_TOQUES = 5;
	private int[] xToque, yToque;

	public ControleVirtual(Context context) {
		super(context);
		iniciar();
	}

	public ControleVirtual(Context context, AttributeSet attrs) {
		super(context, attrs);
		iniciar();
	}

	public ControleVirtual(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
		iniciar();
	}

	@TargetApi(Build.VERSION_CODES.LOLLIPOP)
	public ControleVirtual(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
		super(context, attrs, defStyleAttr, defStyleRes);
		iniciar();
	}

	@SuppressWarnings("deprecation")
	private void iniciar() {
		estadoTeclas = 0;
		rectTeclas = new Rect[QUANTIDADE_TECLAS_NA_TELA];

		for (int i = 0; i < QUANTIDADE_TECLAS_NA_TELA; i++)
			rectTeclas[i] = new Rect();

		xToque = new int[QUANTIDADE_MAXIMA_TOQUES];
		yToque = new int[QUANTIDADE_MAXIMA_TOQUES];
		for (int i = 0; i < QUANTIDADE_MAXIMA_TOQUES; i++) {
			xToque[i] = Integer.MIN_VALUE;
			yToque[i] = Integer.MIN_VALUE;
		}

		Resources resources = getContext().getResources();

		colorPrimary = resources.getColor(R.color.colorPrimary);
		colorPrimaryDark = resources.getColor(R.color.colorPrimaryDark);
		colorAccent = resources.getColor(R.color.colorAccent);
		textoPrimary = resources.getColor(R.color.textoPrimary);
		textoAccent = resources.getColor(R.color.textoAccent);

		paint = new Paint();
		paint.setDither(false);
		paint.setAntiAlias(false);
		paint.setStyle(Paint.Style.FILL);

		textPaint = new TextPaint();
		textPaint.setDither(false);
		textPaint.setAntiAlias(true);
		textPaint.setStyle(Paint.Style.FILL);
		textPaint.setTypeface(Typeface.DEFAULT);
		textPaint.setTextSize(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 16, resources.getDisplayMetrics()));
		textPaint.setTextAlign(Paint.Align.LEFT);
		textPaint.setColor(0xffffffff);
		Paint.FontMetrics metrics = textPaint.getFontMetrics();
		yTexto = (int)-metrics.ascent;

		setClickable(false);
		setFocusable(false);
	}

	@Override
	@ViewDebug.ExportedProperty(category = "drawing")
	public boolean isOpaque() {
		return false;
	}

	public void setListener(Listener listener) {
		this.listener = listener;
	}

	public int getEstadoTeclas() {
		return estadoTeclas;
	}

	@Override
	public int getPaddingLeft() {
		return 0;
	}

	@Override
	public int getPaddingTop() {
		return 0;
	}

	@Override
	public int getPaddingRight() {
		return 0;
	}

	@Override
	public int getPaddingBottom() {
		return 0;
	}

	@Override
	public void setPadding(int left, int top, int right, int bottom) {
	}

	@Override
	protected int getSuggestedMinimumWidth() {
		return TAMANHO_MINIMO;
	}

	@Override
	public int getMinimumWidth() {
		return TAMANHO_MINIMO;
	}

	@Override
	protected int getSuggestedMinimumHeight() {
		return TAMANHO_MINIMO;
	}

	@Override
	public int getMinimumHeight() {
		return TAMANHO_MINIMO;
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		setMeasuredDimension(resolveSize(TAMANHO_MINIMO, widthMeasureSpec), resolveSize(TAMANHO_MINIMO, heightMeasureSpec));
	}

	@Override
	protected void onSizeChanged(int w, int h, int oldw, int oldh) {
		super.onSizeChanged(w, h, oldw, oldh);

		int tamanhoTecla = Math.min(h / 5, w / 9);
		int margem = tamanhoTecla / 5;

		Rect teclaD, teclaS, teclaA, teclaW, teclaShift, teclaEspaco, teclaEsc, teclaE, teclaMouseEsq, tecla, tecla1;

		teclaD = rectTeclas[TECLA_D];
		teclaD.left = w - margem - tamanhoTecla;
		teclaD.top = h - margem - tamanhoTecla;
		teclaD.right = teclaD.left + tamanhoTecla;
		teclaD.bottom = teclaD.top + tamanhoTecla;

		teclaS = rectTeclas[TECLA_S];
		teclaS.left = teclaD.left - tamanhoTecla;
		teclaS.top = teclaD.top;
		teclaS.right = teclaD.left;
		teclaS.bottom = teclaD.bottom;

		teclaA = rectTeclas[TECLA_A];
		teclaA.left = teclaS.left - tamanhoTecla;
		teclaA.top = teclaS.top;
		teclaA.right = teclaS.left;
		teclaA.bottom = teclaS.bottom;

		teclaW = rectTeclas[TECLA_W];
		teclaW.left = teclaS.left;
		teclaW.top = teclaS.top - tamanhoTecla;
		teclaW.right = teclaS.right;
		teclaW.bottom = teclaS.top;

		tecla = rectTeclas[TECLA_WA];
		tecla.left = teclaA.left;
		tecla.top = teclaW.top;
		tecla.right = teclaA.right;
		tecla.bottom = teclaA.top;

		tecla = rectTeclas[TECLA_WD];
		tecla.left = teclaD.left;
		tecla.top = teclaW.top;
		tecla.right = teclaD.right;
		tecla.bottom = teclaD.top;

		teclaEsc = rectTeclas[TECLA_ESC];
		teclaEsc.left = margem;
		teclaEsc.top = margem;
		teclaEsc.right = teclaEsc.left + tamanhoTecla;
		teclaEsc.bottom = teclaEsc.top + tamanhoTecla;

		teclaE = rectTeclas[TECLA_E];
		teclaE.left = teclaEsc.right;
		teclaE.top = teclaEsc.top;
		teclaE.right = teclaE.left + tamanhoTecla;
		teclaE.bottom = teclaEsc.bottom;

		tecla = rectTeclas[TECLA_ENTER];
		tecla.left = teclaE.right;
		tecla.top = teclaE.top;
		tecla.right = tecla.left + tamanhoTecla;
		tecla.bottom = teclaE.bottom;

		tecla = rectTeclas[TECLA_CFG];
		tecla.left = w - margem - tamanhoTecla;
		tecla.top = teclaEsc.top;
		tecla.right = tecla.left + tamanhoTecla;
		tecla.bottom = teclaEsc.bottom;

		teclaShift = rectTeclas[TECLA_SHIFT];
		teclaShift.left = margem;
		teclaShift.top = h - margem - tamanhoTecla;
		teclaShift.right = teclaShift.left + tamanhoTecla;
		teclaShift.bottom = teclaShift.top + tamanhoTecla;

		teclaEspaco = rectTeclas[TECLA_ESPACO];
		teclaEspaco.left = teclaShift.right;
		teclaEspaco.top = teclaShift.top;
		teclaEspaco.right = teclaEspaco.left + (3 * tamanhoTecla);
		teclaEspaco.bottom = teclaShift.bottom;

		teclaMouseEsq = rectTeclas[TECLA_MOUSE_ESQ];
		teclaMouseEsq.left = teclaEspaco.left;
		teclaMouseEsq.top = teclaEspaco.top - tamanhoTecla;
		teclaMouseEsq.right = teclaMouseEsq.left + tamanhoTecla;
		teclaMouseEsq.bottom = teclaEspaco.top;

		tecla = rectTeclas[TECLA_MOUSE_DIR];
		tecla.left = teclaMouseEsq.right;
		tecla.top = teclaMouseEsq.top;
		tecla.right = tecla.left + tamanhoTecla;
		tecla.bottom = teclaMouseEsq.bottom;

		tecla1 = rectTeclas[TECLA_1];
		tecla1.left = teclaEsc.left;
		tecla1.top = teclaEsc.bottom;
		tecla1.right = teclaEsc.right;
		tecla1.bottom = tecla1.top + tamanhoTecla;

		for (int i = TECLA_2; i <= TECLA_8; i++) {
			tecla = rectTeclas[i];
			tecla.left = rectTeclas[i - 1].right;
			tecla.top = tecla1.top;
			tecla.right = tecla.left + tamanhoTecla;
			tecla.bottom = tecla1.bottom;
		}
	}

	private void tratarToques() {
		boolean alterado = false;
		Rect tecla;
		int mascaraAtual;
		int estadoAtual;
		for (int i = 0; i < QUANTIDADE_TECLAS; i++) {
			tecla = rectTeclas[i];
			mascaraAtual = (1 << i);
			estadoAtual = 0;
			for (int p = 0; p < QUANTIDADE_MAXIMA_TOQUES; p++) {
				if (xToque[p] >= 0 && tecla.contains(xToque[p], yToque[p])) {
					estadoAtual = mascaraAtual;
					break;
				}
			}
			if ((estadoTeclas & mascaraAtual) != estadoAtual) {
				estadoTeclas ^= mascaraAtual;
				alterado = true;
			}
		}

		tecla = rectTeclas[TECLA_WA];
		mascaraAtual = (1 << TECLA_W) | (1 << TECLA_A);
		estadoAtual = 0;
		for (int p = 0; p < QUANTIDADE_MAXIMA_TOQUES; p++) {
			if (xToque[p] >= 0 && tecla.contains(xToque[p], yToque[p])) {
				estadoAtual = mascaraAtual;
				break;
			}
		}
		if (estadoAtual != 0 && (estadoTeclas & mascaraAtual) != estadoAtual) {
			estadoTeclas |= mascaraAtual;
			alterado = true;
		}

		tecla = rectTeclas[TECLA_WD];
		mascaraAtual = (1 << TECLA_W) | (1 << TECLA_D);
		estadoAtual = 0;
		for (int p = 0; p < QUANTIDADE_MAXIMA_TOQUES; p++) {
			if (xToque[p] >= 0 && tecla.contains(xToque[p], yToque[p])) {
				estadoAtual = mascaraAtual;
				break;
			}
		}
		if (estadoAtual != 0 && (estadoTeclas & mascaraAtual) != estadoAtual) {
			estadoTeclas |= mascaraAtual;
			alterado = true;
		}

		if (listener != null) {
			boolean pressionada = false;
			tecla = rectTeclas[TECLA_CFG];
			for (int p = 0; p < QUANTIDADE_MAXIMA_TOQUES; p++) {
				if (xToque[p] >= 0 && tecla.contains(xToque[p], yToque[p])) {
					pressionada = true;
					if (teclaCfg == 0) {
						teclaCfg = 1;
						listener.teclaConfiguracao(this);
					}
					break;
				}
			}
			if (!pressionada)
				teclaCfg = 0;
		}

		if (alterado) {
			invalidate();
			if (listener != null)
				listener.estadoTeclasAlterado(this);
		}
	}

	@SuppressLint("ClickableViewAccessibility")
	@Override
	public boolean onTouchEvent(MotionEvent event) {
		if (!isEnabled())
			return false;

		int i, id;

		switch (event.getActionMasked()) {
		case MotionEvent.ACTION_MOVE:
			for (i = event.getPointerCount() - 1; i >= 0; i--) {
				id = event.getPointerId(i);
				if (id >= QUANTIDADE_MAXIMA_TOQUES)
					continue;
				xToque[id] = (int)event.getX(i);
				yToque[id] = (int)event.getY(i);
			}
			tratarToques();
			break;
		case MotionEvent.ACTION_DOWN:
		case MotionEvent.ACTION_POINTER_DOWN:
			i = event.getActionIndex();
			id = event.getPointerId(i);
			if (id >= QUANTIDADE_MAXIMA_TOQUES)
				break;
			xToque[id] = (int)event.getX(i);
			yToque[id] = (int)event.getY(i);
			tratarToques();
			break;
		case MotionEvent.ACTION_UP:
		case MotionEvent.ACTION_POINTER_UP:
		case MotionEvent.ACTION_CANCEL:
			id = event.getPointerId(event.getActionIndex());
			//System.out.println("UP " + total + " / " + i + " / " + id + " / " + event.getActionMasked());
			if (id >= QUANTIDADE_MAXIMA_TOQUES)
				break;
			xToque[id] = Integer.MIN_VALUE;
			yToque[id] = Integer.MIN_VALUE;
			tratarToques();
			break;
		}
		return true;
	}

	@Override
	protected void onDraw(Canvas canvas) {
		Rect tecla;
		for (int i = 0; i < QUANTIDADE_TECLAS; i++) {
			if ((estadoTeclas & (1 << i)) == 0) {
				textPaint.setColor(textoAccent);
				paint.setColor(colorAccent);
			} else {
				textPaint.setColor(textoPrimary);
				paint.setColor(colorPrimary);
			}
			tecla = rectTeclas[i];
			canvas.drawRect(tecla.left + 1, tecla.top + 1, tecla.right - 1, tecla.bottom - 1, paint);
			canvas.drawText(TEXTO_TECLAS[i], tecla.left + 4, tecla.top + 3 + yTexto, textPaint);
		}
		textPaint.setColor(textoAccent);
		paint.setColor(colorPrimaryDark);
		tecla = rectTeclas[TECLA_WA];
		canvas.drawRect(tecla.left + 1, tecla.top + 1, tecla.right - 1, tecla.bottom - 1, paint);
		canvas.drawText(TEXTO_TECLAS[TECLA_WA], tecla.left + 4, tecla.top + 3 + yTexto, textPaint);
		tecla = rectTeclas[TECLA_WD];
		canvas.drawRect(tecla.left + 1, tecla.top + 1, tecla.right - 1, tecla.bottom - 1, paint);
		canvas.drawText(TEXTO_TECLAS[TECLA_WD], tecla.left + 4, tecla.top + 3 + yTexto, textPaint);
		tecla = rectTeclas[TECLA_CFG];
		canvas.drawRect(tecla.left + 1, tecla.top + 1, tecla.right - 1, tecla.bottom - 1, paint);
		canvas.drawText(TEXTO_TECLAS[TECLA_CFG], tecla.left + 4, tecla.top + 3 + yTexto, textPaint);
	}
}
