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

package br.com.carlosrafaelgn.controleudp;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.util.Patterns;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.Toast;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;

import br.com.carlosrafaelgn.controleudp.ui.ControleVirtual;

public class MainActivity extends Activity implements ControleVirtual.Listener, SensorEventListener, DialogInterface.OnShowListener, Handler.Callback {

	private static final String PREF_ARQUIVO = "Cfg.cfg";
	private static final String KEY_IP = "Ip";
	private static final String KEY_PORTA = "Porta";

	private String ip;
	private int porta, valorX, valorY;
	private float valorXReal, valorYReal;
	private SensorManager sensorManager;
	private Sensor sensor;
	private HandlerThread handlerThread;
	private Handler handler;
	private DatagramSocket socket;
	private byte[] buffer;
	private DatagramPacket packet;
	private ControleVirtual controleVirtual;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

		SharedPreferences sharedPreferences = getSharedPreferences(PREF_ARQUIVO, MODE_PRIVATE);
		ip = sharedPreferences.getString(KEY_IP, "");
		porta = sharedPreferences.getInt(KEY_PORTA, 6200);

		controleVirtual = findViewById(R.id.controleVirtual);
		controleVirtual.setListener(this);
	}

	@Override
	protected void onStart() {
		super.onStart();

		exibirDialog();
	}

	@Override
	protected void onStop() {
		super.onStop();

		pararTransmissao();
	}

	@Override
	public void estadoTeclasAlterado(ControleVirtual controleVirtual) {
		enviarMensagem();
	}

	@Override
	public void teclaConfiguracao(ControleVirtual controleVirtual) {
		exibirDialog();
	}

	@Override
	public void onSensorChanged(SensorEvent event) {
		// Inverte X e Y (values[0] e values[1]), porque o app está deitado (landscape)
		valorXReal = (0.75f * valorXReal) + (0.25f * event.values[1]);
		int x = ((valorXReal <= -7) ? -127 : ((valorXReal >= 7) ? 127 : (int)(valorXReal * 127.0f / 7.0f)));
		valorX = 127 + ((x > -15 && x < 15) ? 0 : x);

		valorYReal = (0.75f * valorYReal) + (0.25f * event.values[0]);
		// -4.9 para compensar pelo ângulo que a pessoa segura o celular deitado
		float yReal = valorYReal - 4.9f;
		int y = ((yReal <= -5) ? 50 : ((yReal >= 5) ? -50 : (int)(yReal * -50.0f / 5.0f)));
		valorY = 127 + ((y > -10 && y < 10) ? 0 : y);

		enviarMensagem();
	}

	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {

	}

	private void exibirDialog() {
		@SuppressLint("InflateParams")
		AlertDialog alertDialog = new AlertDialog.Builder(this)
			.setCancelable(false)
			.setNegativeButton(R.string.cancelar, null)
			.setPositiveButton(R.string.ok, null)
			.setView(getLayoutInflater().inflate(R.layout.dialog, null))
			.setTitle(R.string.configuracoes)
			.create();

		alertDialog.setCanceledOnTouchOutside(false);

		alertDialog.setOnShowListener(this);

		alertDialog.show();
	}

	@Override
	public void onShow(DialogInterface dialog) {
		final AlertDialog alertDialog = (AlertDialog)dialog;
		final EditText txtIP = alertDialog.findViewById(R.id.txtIP);
		final EditText txtPorta = alertDialog.findViewById(R.id.txtPorta);

		txtIP.setText(ip == null ? "" : ip);
		txtPorta.setText(porta <= 0 ? "" : Integer.toString(porta));

		alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				String ip = txtIP.getText().toString();
				if (!Patterns.IP_ADDRESS.matcher(ip).matches())
					return;

				int porta;
				try {
					porta = Integer.parseInt(txtPorta.getText().toString());
				} catch (NumberFormatException ex) {
					return;
				}
				if (porta <= 0 || porta > 65535)
					return;

				alertDialog.dismiss();

				SharedPreferences sharedPreferences = getSharedPreferences(PREF_ARQUIVO, MODE_PRIVATE);
				SharedPreferences.Editor editor = sharedPreferences.edit();
				editor.putString(KEY_IP, ip);
				editor.putInt(KEY_PORTA, porta);
				editor.apply();

				MainActivity.this.ip = ip;
				MainActivity.this.porta = porta;

				iniciarTransmissao();
			}
		});
	}

	private void iniciarTransmissao() {
		pararTransmissao();

		valorX = 0;
		valorY = 0;
		valorXReal = 0.0f;
		valorYReal = 0.0f;

		try {
			sensorManager = (SensorManager)getSystemService(SENSOR_SERVICE);
			sensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
			sensorManager.registerListener(this, sensor, 50000);
		} catch (Exception e) {
			Toast.makeText(this, "Erro: " + e.getMessage(), Toast.LENGTH_LONG).show();
			pararTransmissao();
			return;
		}

		handlerThread = new HandlerThread("UDP");
		handlerThread.start();
		handler = new Handler(handlerThread.getLooper(), this);

		try {
			socket = new DatagramSocket();
			buffer = new byte[ControleVirtual.QUANTIDADE_TECLAS + 2];
			packet = new DatagramPacket(buffer, 0, buffer.length, new InetSocketAddress(InetAddress.getByName(ip), porta));
		} catch (IOException e) {
			Toast.makeText(this, "Erro: " + e.getMessage(), Toast.LENGTH_LONG).show();
			pararTransmissao();
		}
	}

	private void pararTransmissao() {
		if (sensorManager != null) {
			sensorManager.unregisterListener(this);
			sensorManager = null;
		}

		sensor = null;
		handler = null;

		if (handlerThread != null) {
			handlerThread.quit();
			while (handlerThread.isAlive()) {
				try {
					Thread.sleep(10);
				} catch (InterruptedException e) {
					// Apenas ignora, e espera...
				}
			}
			handlerThread = null;
		}

		if (socket != null) {
			socket.close();
			socket = null;
		}

		buffer = null;
	}

	private void enviarMensagem() {
		if (handler != null)
			handler.sendMessage(Message.obtain(handler, controleVirtual.getEstadoTeclas(), valorX, valorY));
	}

	@Override
	public boolean handleMessage(Message msg) {
		int estadoTeclas = msg.what;
		int valorX = msg.arg1;
		int valorY = msg.arg2;

		for (int i = 0; i < ControleVirtual.QUANTIDADE_TECLAS; i++)
			buffer[i] = (byte)((estadoTeclas >> i) & 1);

		buffer[ControleVirtual.QUANTIDADE_TECLAS] = (byte)valorX;
		buffer[ControleVirtual.QUANTIDADE_TECLAS + 1] = (byte)valorY;

		try {
			socket.send(packet);
		} catch (IOException e) {
			// Se tivéssemos um callback de erro, poderíamos tratar essa exceção lá
		}

		return true;
	}
}
