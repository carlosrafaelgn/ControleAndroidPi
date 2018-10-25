#
# MIT License
#
# Copyright (c) 2018 Carlos Rafael Gimenes das Neves
#
# Permission is hereby granted, free of charge, to any person obtaining a copy
# of this software and associated documentation files (the "Software"), to deal
# in the Software without restriction, including without limitation the rights
# to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
# copies of the Software, and to permit persons to whom the Software is
# furnished to do so, subject to the following conditions:
#
# The above copyright notice and this permission notice shall be included in all
# copies or substantial portions of the Software.
#
# THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
# IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
# FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
# AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
# LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
# OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
# SOFTWARE.
#
# https://github.com/carlosrafaelgn/ControleAndroidPi
#

#
# Estamos utilizando o pacote python-uinput para emular o teclado
# e o mouse no Linux (Raspberry Pi 3):
# https://github.com/tuomasjjrasanen/python-uinput
#
# Depois de instalado o pacote, com o comando
#
# sudo pip3 install python-uinput
#
# e *antes* de executar o script, devemos garantir que o módulo
# uinput, do kernel do Linux, esteja executando:
#
# sudo modprobe uinput
#
# O comando acima precisa ser executado ao menos uma vez depois
# do Linux ter sido ligado
#
# Para não precisar digitar esse comando toda vez, manualmente,
# é necessário que o módulo uinput seja adicionado à /etc/modules
# fazendo com que o módulo passe a ser carregado automaticamente,
# durante o carregamento do Linux
#
# Para executar o script abaixo, que faz uso do uinput, normalmente
# é necessario executar com as permissões de root:
#
# sudo python3 emulador.py
#

from time import sleep

# Para emular o teclado e o mouse
import uinput

# Para a comunicação UDP
import socket

# Uma lista com todas as teclas utilizadas durante a emulação
entradasUtilizadas = [
    uinput.KEY_W, uinput.KEY_A, uinput.KEY_S, uinput.KEY_D,
    uinput.KEY_E, uinput.KEY_LEFTSHIFT, uinput.KEY_SPACE, uinput.KEY_ENTER, uinput.KEY_ESC,
    uinput.BTN_LEFT, uinput.BTN_RIGHT,
    uinput.KEY_1, uinput.KEY_2, uinput.KEY_3, uinput.KEY_4, uinput.KEY_5, uinput.KEY_6, uinput.KEY_7, uinput.KEY_8,
    uinput.REL_X, uinput.REL_Y
]

# Quantas entradas da lista são teclas de verdade
entradasTeclas = 19

# Inicialmente, todas as nossas teclas estão soltas
estadoTeclas = [
    0, 0, 0, 0,
    0, 0, 0, 0, 0,
    0, 0,
    0, 0, 0, 0, 0, 0, 0, 0
    # Não é necessário armazenar a posição do cursor do mouse
]

with uinput.Device(entradasUtilizadas) as emulador:
	# Cria um socket UDP
    with socket.socket(socket.AF_INET, socket.SOCK_DGRAM, socket.IPPROTO_UDP) as s:
        try:
            s.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
            s.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEPORT, 1)
        except AttributeError:
            pass

		# Vamos receber pacotes por qualquer IP, na porta 6200
        s.bind(('', 6200))

        while True:
            dados = s.recv(21)

            # Só nos interessa pacotes com 21 bytes (nosso "protocolinho")
            if len(dados) != 21:
                continue

            for tecla in range(entradasTeclas):
                # Se o estado da tecla não foi alterado, ignora e apenas
                # pula para a próxima tecla
                if estadoTeclas[tecla] == dados[tecla]:
                    continue
                estadoTeclas[tecla] = dados[tecla]
                emulador.emit(entradasUtilizadas[tecla], estadoTeclas[tecla])

            # Para o mouse, vamos emular movimentos relativos e não absolutos
			emulador.emit(uinput.REL_X, dados[entradasTeclas] - 127)
            emulador.emit(uinput.REL_Y, dados[entradasTeclas + 1] - 127)
