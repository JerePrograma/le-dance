package ledance.repositorios;

import ledance.entidades.Rol;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ledance.entidades.Usuario;

import java.util.List;
import java.util.Optional;

@Repository
public interface UsuarioRepositorio extends JpaRepository<Usuario, Long> {

    Optional<Usuario> findByNombreUsuario(String nombreUsuario);

    List<Usuario> findByRolAndActivo(Rol rol, Boolean activo);

    List<Usuario> findByRol(Rol rol);

    List<Usuario> findByActivo(Boolean activo);

    List<Usuario> findByActivoTrue();
}