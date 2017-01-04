package br.com.libertsolutions.libertvendas.app.presentation.listapedidos;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import br.com.libertsolutions.libertvendas.app.R;
import br.com.libertsolutions.libertvendas.app.presentation.fragment.LibertVendasFragment;
import br.com.libertsolutions.libertvendas.app.presentation.widget.TabAdapter;
import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * @author Filipe Bezerra
 */
public class TabsFragment extends LibertVendasFragment {

    public static final String TAG = TabsFragment.class.getName();

    @BindView(R.id.view_pager) protected ViewPager mViewPager;

    private AppBarLayout mAppBarLayoutActivity;

    private TabLayout mTabLayout;

    private TabAdapter mTabAdapter;

    public static TabsFragment newInstance() {
        return new TabsFragment();
    }

    @Override protected int provideContentViewResource() {
        return R.layout.fragment_tabs;
    }

    @Override public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        if (mAppBarLayoutActivity == null) {
            mAppBarLayoutActivity = ButterKnife.findById(getActivity(), R.id.app_bar);
        }

        if (mTabLayout == null) {
            mTabLayout = (TabLayout) LayoutInflater.from(getContext())
                    .inflate(R.layout.tab_layout, mAppBarLayoutActivity, false);

            mTabAdapter = new TabAdapter(getChildFragmentManager());
            mTabAdapter.addFragment(ListaPedidosFragment.newInstance(false),
                    getString(R.string.title_fragment_todos_pedidos));
            mTabAdapter.addFragment(ListaPedidosFragment.newInstance(true),
                    getString(R.string.title_fragment_pedidos_nao_enviados));
            mViewPager.setAdapter(mTabAdapter);

            mTabLayout.setupWithViewPager(mViewPager);
        }

        mAppBarLayoutActivity.addView(mTabLayout);
    }

    @Override public void onActivityResult(final int requestCode, final int resultCode,
            final Intent data) {
        for (Fragment fragment : mTabAdapter.getFragments()) {
            fragment.onActivityResult(requestCode, resultCode, data);
        }
    }

    @Override public void onDestroyView() {
        mAppBarLayoutActivity.removeView(mTabLayout);
        super.onDestroyView();
    }
}
