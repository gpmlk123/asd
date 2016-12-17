package br.com.libertsolutions.libertvendas.app.presentation.cadastropedido.finalizando;

import br.com.libertsolutions.libertvendas.app.data.formaspagamento.FormaPagamentoRepository;
import br.com.libertsolutions.libertvendas.app.data.pedidos.PedidoRepository;
import br.com.libertsolutions.libertvendas.app.data.util.ApiUtils;
import br.com.libertsolutions.libertvendas.app.domain.pojo.Cliente;
import br.com.libertsolutions.libertvendas.app.domain.pojo.Empresa;
import br.com.libertsolutions.libertvendas.app.domain.pojo.FormaPagamento;
import br.com.libertsolutions.libertvendas.app.domain.pojo.ItemPedido;
import br.com.libertsolutions.libertvendas.app.domain.pojo.Pedido;
import br.com.libertsolutions.libertvendas.app.domain.pojo.TabelaPreco;
import br.com.libertsolutions.libertvendas.app.domain.pojo.Vendedor;
import br.com.libertsolutions.libertvendas.app.domain.vo.ProdutoVo;
import br.com.libertsolutions.libertvendas.app.presentation.base.BasePresenter;
import br.com.libertsolutions.libertvendas.app.presentation.listaclientes.ClienteSelecionadoEvent;
import br.com.libertsolutions.libertvendas.app.presentation.listaprodutos.ProdutosSelecionadosEvent;
import br.com.libertsolutions.libertvendas.app.presentation.login.UsuarioLogadoEvent;
import br.com.libertsolutions.libertvendas.app.presentation.resources.FinalizandoPedidoResourcesRepository;
import br.com.libertsolutions.libertvendas.app.presentation.util.FormattingUtils;
import br.com.libertsolutions.libertvendas.app.presentation.util.ObservableUtils;
import br.com.libertsolutions.libertvendas.app.presentation.util.StringUtils;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import timber.log.Timber;

import static org.greenrobot.eventbus.ThreadMode.MAIN;

/**
 * @author Filipe Bezerra
 */
class FinalizandoPedidoPresenter extends BasePresenter<FinalizandoPedidoContract.View>
        implements FinalizandoPedidoContract.Presenter {

    private final FormaPagamentoRepository mFormaPagamentoRepository;

    private final PedidoRepository mPedidoRepository;

    private final FinalizandoPedidoResourcesRepository mResourcesRepository;

    private Pedido mPedidoEmEdicao;

    private Calendar mDataEmissao = Calendar.getInstance();

    private Cliente mClienteSelecionado;

    private TabelaPreco mTabelaPrecoPadrao;

    private List<ProdutoVo> mProdutosSelecionados;

    private List<FormaPagamento> mFormasPagamentoList;

    private Vendedor mVendedorLogado;

    private Empresa mEmpresaLogada;

    FinalizandoPedidoPresenter(FinalizandoPedidoDependencyContainer pDependencyContainer) {
        mFormaPagamentoRepository = pDependencyContainer.getFormaPagamentoRepository();
        mPedidoRepository = pDependencyContainer.getPedidoRepository();
        mResourcesRepository = pDependencyContainer.getResourcesRepository();
    }

    @Override public void initializeView(Pedido pPedidoFromExtra) {
        mPedidoEmEdicao = pPedidoFromExtra;
        initializeViewFields();
        initializeViewRequiredFields();
        displayDataEmissao();
        loadFormasPagamento();
        initializeFieldsIfEmEdicao();
    }

    private void initializeViewFields() {
        List<Integer> viewIds = new ArrayList<>();
        viewIds.add(mResourcesRepository.obtainDataEmissaoViewId());
        viewIds.add(mResourcesRepository.obtainClienteViewId());
        viewIds.add(mResourcesRepository.obtainTotalProdutosViewId());
        viewIds.add(mResourcesRepository.obtainDescontoViewId());
        viewIds.add(mResourcesRepository.obtainFormaPagamentoViewId());
        viewIds.add(mResourcesRepository.obtainObservacaoViewId());
        getView().setViewFields(viewIds);
    }

    private void initializeViewRequiredFields() {
        List<Integer> requiredViewIds = new ArrayList<>();
        requiredViewIds.add(mResourcesRepository.obtainDataEmissaoViewId());
        requiredViewIds.add(mResourcesRepository.obtainClienteViewId());
        requiredViewIds.add(mResourcesRepository.obtainFormaPagamentoViewId());
        getView().setRequiredFields(requiredViewIds);
    }

    private void loadFormasPagamento() {
        Observable<List<FormaPagamento>> formasPagamentoFromMemoryCache = ObservableUtils
                .toObservable(mFormasPagamentoList);

        Observable<List<FormaPagamento>> formasPagamentoFromDiskCache = mFormaPagamentoRepository
                .list()
                .doOnNext(pFormasPagamentoList -> mFormasPagamentoList = pFormasPagamentoList);

        addSubscription(Observable
                .concat(formasPagamentoFromMemoryCache, formasPagamentoFromDiskCache)
                .firstOrDefault(Collections.emptyList())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        pFormasPagamentoList -> {
                            getView().displayFormasPagamento(mFormasPagamentoList);
                            initializeFormasPagamentoFieldIfEmEdicao();
                        },

                        Timber::e
                ));
    }

    private void initializeFormasPagamentoFieldIfEmEdicao() {
        if (isEditing()) {
            int indexOf = mFormasPagamentoList.indexOf(mPedidoEmEdicao.getFormaPagamento());
            getView().setViewValue(mResourcesRepository.obtainFormaPagamentoViewId(), indexOf);
        }
    }

    private void initializeFieldsIfEmEdicao() {
        if (isEditing()) {
            getView().changeTitle(mResourcesRepository.obtainStringTitleEditandoPedido());

            displayDataEmissao();

            getView().setViewValue(
                    mResourcesRepository.obtainClienteViewId(),
                    mPedidoEmEdicao.getCliente().getNome());

            displayTotalProdutos(calculateTotalProdutosPedido());

            getView().setViewValue(
                    mResourcesRepository.obtainDescontoViewId(),
                    String.valueOf(mPedidoEmEdicao.getDesconto()));

            if (!StringUtils.isEmpty(mPedidoEmEdicao.getObservacao())) {
                getView().setViewValue(
                        mResourcesRepository.obtainObservacaoViewId(),
                        mPedidoEmEdicao.getObservacao());
            }
        }
    }

    private double calculateTotalProdutosPedido() {
        double totalProdutosPedido = 0;
        if (isEditing()) {
            List<ItemPedido> itens = mPedidoEmEdicao.getItens();

            for (ItemPedido item : itens) {
                totalProdutosPedido += item.getSubTotal();
            }
        }
        return totalProdutosPedido;
    }

    @Override public void handleDataEmissaoTouched() {
        getView().showCalendarPicker(mDataEmissao);
    }

    @Override public void handleDateSelected(int pYear, int pMonthOfYear, int pDayOfMonth) {
        mDataEmissao.set(Calendar.YEAR, pYear);
        mDataEmissao.set(Calendar.MONTH, pMonthOfYear);
        mDataEmissao.set(Calendar.DAY_OF_MONTH, pDayOfMonth);
        displayDataEmissao();
    }

    private void displayDataEmissao() {
        long dataEmissao = isEditing() ?
                mPedidoEmEdicao.getDataEmissao() : mDataEmissao.getTimeInMillis();
        getView().setViewValue(mResourcesRepository.obtainDataEmissaoViewId(),
                FormattingUtils.convertMillisecondsToDateAsString(dataEmissao));
    }

    @Subscribe(sticky = true) public void onProdutosSelecionadosEvent(
            ProdutosSelecionadosEvent pEvent) {
        List<ProdutoVo> produtoVoList = pEvent.getProdutoVoList();
        TabelaPreco tabelaPreco = pEvent.getTabelaPreco();
        if ((produtoVoList != null && !produtoVoList.isEmpty()) && tabelaPreco != null) {
            mProdutosSelecionados = produtoVoList;
            mTabelaPrecoPadrao = tabelaPreco;
            displayTotalProdutos(calculateTotalProdutosSelecionados());
        }

        EventBus.getDefault().removeStickyEvent(pEvent);
    }

    @Subscribe(threadMode = MAIN, sticky = true) public void onUsuarioLogadoEvent(
            UsuarioLogadoEvent pEvent) {
        mVendedorLogado = pEvent.getVendedor();
        mEmpresaLogada = pEvent.getEmpresa();
    }

    private void displayTotalProdutos(double pTotalProdutos) {
        getView().setViewValue(mResourcesRepository.obtainTotalProdutosViewId(),
                FormattingUtils.formatAsDinheiro(pTotalProdutos));
    }

    @Subscribe(sticky = true) public void onClienteSelecionadoEvent(
            ClienteSelecionadoEvent pEvent) {
        Cliente cliente = pEvent.getCliente();
        if (cliente != null) {
            mClienteSelecionado = cliente;
            getView().setViewValue(mResourcesRepository.obtainClienteViewId(), cliente.getNome());
        }

        EventBus.getDefault().removeStickyEvent(pEvent);
    }

    @Override public void handleActionSave() {
        getView().hideRequiredMessages();

        if (!getView().hasEmptyRequiredFields()) {
            if (validateDesconto()) {
                addSubscription(mPedidoRepository.save(pedidoFromFields())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(
                                this::notifyPedidoSalvo,

                                Timber::e
                        )
                );
            }
        } else {
            getView().displayRequiredFieldMessages();
        }
    }

    private boolean validateDesconto() {
        final String descontoStr = getView().getViewStringValue(
                mResourcesRepository.obtainDescontoViewId());

        if (StringUtils.isEmpty(descontoStr)) {
            return true;
        }

        final double desconto = Double.valueOf(descontoStr);

        if (desconto == 0) {
            return true;
        }

        if (!mVendedorLogado.isAplicaDesconto()) {
            getView().displayValidationErrorForDesconto(
                    mResourcesRepository.obtainStringMessageVendedorNaoPermitidoAplicarDesconto());
            return false;
        }

        int positionFormaPagamento = getView()
                .getViewPositionValue(mResourcesRepository.obtainFormaPagamentoViewId());

        final FormaPagamento formaPagamento = getItemFromList(
                mFormasPagamentoList, positionFormaPagamento);

        if (formaPagamento == null) {
            return true;
        }

        final float percentualDesconto = formaPagamento.getPercentualDesconto();

        if (percentualDesconto == 0) {
            getView().displayValidationErrorForDesconto(
                    mResourcesRepository.obtainStringMessageValorDescontoNaoPermitido());
            return false;
        }

        final double totalProdutos = calculateTotalProdutosSelecionados();
        final double percentualAplicadoVenda = desconto * 100 / totalProdutos;

        if (percentualAplicadoVenda > percentualDesconto) {
            getView().displayValidationErrorForDesconto(
                    mResourcesRepository
                            .obtainStringMessageValorDescontoNaoPermitido());
            return false;
        }

        return true;
    }

    private double calculateTotalProdutosSelecionados() {
        double totalProdutos = 0;
        for (ProdutoVo vo : mProdutosSelecionados) {
            totalProdutos += vo.getTotalProdutos();
        }
        return totalProdutos;
    }

    private Pedido pedidoFromFields() {
        final long dataEmissao = mDataEmissao.getTimeInMillis();

        String descontoStr = getView().getViewStringValue(
                mResourcesRepository.obtainDescontoViewId());
        final double desconto = StringUtils.isEmpty(descontoStr) ? 0 : Double.valueOf(descontoStr);

        int positionFormaPagamento = getView().getViewPositionValue(
                mResourcesRepository.obtainFormaPagamentoViewId());
        final FormaPagamento formaPagamento
                = getItemFromList(mFormasPagamentoList, positionFormaPagamento);

        final String observacao = getView().getViewStringValue(
                mResourcesRepository.obtainObservacaoViewId());

        List<ItemPedido> itensPedido = new ArrayList<>();
        for (ProdutoVo vo : mProdutosSelecionados) {
            itensPedido.add(
                    ItemPedido.novoItem(
                            vo.getPreco(),
                            vo.getQuantidadeAdicionada(),
                            vo.getTotalProdutos(),
                            vo.getProduto()
                    )
            );
        }

        final String cnpjEmpresa = mEmpresaLogada.getCnpj();
        final String cpfCnpjVendedor = mVendedorLogado.getCpfCnpj();

        if (isEditing()) {
            return new Pedido(
                    mPedidoEmEdicao.getId(),
                    mPedidoEmEdicao.getIdPedido(),
                    mPedidoEmEdicao.getTipo(),
                    mPedidoEmEdicao.getNumero(),
                    mPedidoEmEdicao.getStatus(),
                    dataEmissao,
                    desconto,
                    formaPagamento.getPercentualDesconto(),
                    observacao,
                    mClienteSelecionado,
                    formaPagamento,
                    mTabelaPrecoPadrao,
                    itensPedido,
                    ApiUtils.formatApiDateTime(System.currentTimeMillis()),
                    cnpjEmpresa,
                    cpfCnpjVendedor
            );
        } else {
            return Pedido.novoPedido(
                    dataEmissao,
                    desconto,
                    formaPagamento.getPercentualDesconto(),
                    observacao,
                    mClienteSelecionado,
                    formaPagamento,
                    mTabelaPrecoPadrao,
                    itensPedido,
                    cnpjEmpresa,
                    cpfCnpjVendedor
            );
        }
    }

    private void notifyPedidoSalvo(final Pedido pPedido) {
        if (isEditing()) {
            getView().resultPedidoEditado(pPedido);
        } else {
            EventBus.getDefault().post(NovoPedidoEvent.newEvent(pPedido));
            getView().finishView();
        }
    }

    private boolean isEditing() {
        return mPedidoEmEdicao != null;
    }

    private <T> T getItemFromList(List<T> list, int position) {
        if (position >= 0 && position < list.size()) {
            return list.get(position);
        }
        return null;
    }
}
