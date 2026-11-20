package org.example.seguridad;

import java.time.Instant;
import java.util.Objects;

import javax.persistence.EntityManager;
import javax.persistence.LockModeType;

import org.example.controllers.LogtoController.Usuario;

public final class ContactosLogto {

  private ContactosLogto() {}

  // Llamar únicamente después de validar el ID token de Logto.
  public static void sincronizar(
      EntityManager em,
      Usuario usuario
  ) {
    Objects.requireNonNull(
        em,
        "Falta el contexto de persistencia"
    );
    Objects.requireNonNull(
        usuario,
        "Falta el usuario validado"
    );

    var tx = em.getTransaction();

    if (tx.isActive()) {
      throw new IllegalStateException(
          "La sincronización de Logto requiere su propia transacción"
      );
    }

    try {
      tx.begin();

      String cuentaId = CuentaWeb.clave(usuario);
      boolean administrador = SesionWeb.esAdmin(usuario);

      ContactoCuenta contacto = em.find(
          ContactoCuenta.class,
          cuentaId,
          LockModeType.PESSIMISTIC_WRITE
      );

      // Donantes y entidades siguen creando su contacto
      // durante el registro o desde Mi perfil.
      if (contacto == null && !administrador) {
        tx.commit();
        return;
      }

      boolean nuevo = contacto == null;

      if (nuevo) {
        contacto = new ContactoCuenta();
        contacto.id = cuentaId;
        contacto.pais = "AR";
        contacto.canal = ContactoCuenta.Canal.MAIL;
      }

      String email = Objects.toString(
          usuario.email(),
          ""
      ).trim();

      boolean correoValido = usuario.emailVerificado()
          && !email.isBlank()
          && email.length() <= 320;

      // No conservar una dirección anterior que Logto ya no verifica.
      contacto.email = correoValido ? email : "";
      contacto.recibeAvisosAdministracion = administrador;
      contacto.actualizado = Instant.now();

      // El canal, país y teléfono existentes se conservan.
      if (nuevo) {
        em.persist(contacto);
      }

      tx.commit();

    } catch (RuntimeException e) {
      if (tx.isActive()) {
        tx.rollback();
      }

      em.clear();
      throw e;
    }
  }
}