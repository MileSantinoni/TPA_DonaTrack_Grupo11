package org.example.dominio.catalogo;

public enum TipoAtributo {
  PERECEDERO,       // Exige fecha de vencimiento
  CON_ESTADO,       // Exige indicar si es nuevo o usado
  NO_PERECEDERO     // No exige datos extra
}
