package br.com.libertsolutions.libertvendas.app.data.pedido;

import br.com.libertsolutions.libertvendas.app.data.repository.Repository;
import br.com.libertsolutions.libertvendas.app.domain.pojo.Pedido;
import java.util.List;
import rx.Observable;

/**
 * @author Filipe Bezerra
 */
public interface PedidoRepository extends Repository<Pedido, Integer> {

    Observable<List<Pedido>> findByStatus(int status);
}
