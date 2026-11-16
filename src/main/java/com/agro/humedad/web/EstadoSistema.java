package com.agro.humedad.web;

import com.agro.humedad.concurrencia.ActorLecturas;

public record EstadoSistema(int nucleos, long lecturasRecibidas, int parcelasEnCache,
                            boolean simulando, ActorLecturas.Estado actor) {
}
