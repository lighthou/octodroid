package com.example.octodroid.views.adapters;

import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;

import com.example.octodroid.data.GitHub;
import com.example.octodroid.views.components.LinearLayoutLoadMoreListener;
import com.example.octodroid.views.helpers.ToastHelper;
import com.example.octodroid.views.holders.ProgressViewHolder;
import com.example.octodroid.views.holders.RepositoryItemViewHolder;
import com.jakewharton.rxbinding.view.RxView;
import com.rejasupotaro.octodroid.http.Response;
import com.rejasupotaro.octodroid.models.Repository;
import com.rejasupotaro.octodroid.models.SearchResult;

import java.util.ArrayList;
import java.util.List;

import rx.Observable;
import rx.Subscriber;
import rx.subjects.BehaviorSubject;

public class SearchResultAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private static class ViewType {
        private static final int ITEM = 1;
        private static final int FOOTER = 2;
    }

    private RecyclerView recyclerView;
    private List<Repository> repositories = new ArrayList<>();
    private BehaviorSubject<Observable<Response<SearchResult<Repository>>>> responseSubject;
    private Observable<Response<SearchResult<Repository>>> pagedResponse;

    public SearchResultAdapter(RecyclerView recyclerView) {
        this.recyclerView = recyclerView;
        recyclerView.setVisibility(View.GONE);

        LinearLayoutManager layoutManager = new LinearLayoutManager(recyclerView.getContext());
        recyclerView.setHasFixedSize(false);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.addOnScrollListener(new LinearLayoutLoadMoreListener(layoutManager) {
            @Override
            public void onLoadMore() {
                if (pagedResponse != null) {
                    responseSubject.onNext(pagedResponse);
                }
            }
        });
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        if (viewType == ViewType.FOOTER) {
            return ProgressViewHolder.create(parent);
        } else {
            return RepositoryItemViewHolder.create(parent);
        }
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder viewHolder, int position) {
        switch (getItemViewType(position)) {
            case ViewType.FOOTER:
                // do nothing
                break;
            default:
                Repository repository = repositories.get(position);
                ((RepositoryItemViewHolder) viewHolder).bind(repository);
                break;
        }
    }

    @Override
    public int getItemViewType(int position) {
        if (repositories.size() == 0 || position == repositories.size()) {
            return ViewType.FOOTER;
        } else {
            return ViewType.ITEM;
        }
    }

    @Override
    public int getItemCount() {
        return repositories.size() + 1;
    }

    public void clear() {
        repositories.clear();
        notifyDataSetChanged();
    }

    public void submit(String query) {
        clear();
        recyclerView.setVisibility(View.VISIBLE);

        responseSubject = BehaviorSubject.create(GitHub.client().searchRepositories(query));
        responseSubject
                .takeUntil(RxView.detaches(recyclerView))
                .flatMap(r -> r)
                .subscribe(new ResponseSubscriber());
    }

    private class ResponseSubscriber extends Subscriber<Response<SearchResult<Repository>>> {

        @Override
        public void onCompleted() {
            unsubscribe();
        }

        @Override
        public void onError(Throwable e) {
            ToastHelper.showError(recyclerView.getContext());
        }

        @Override
        public void onNext(Response<SearchResult<Repository>> r) {
            if (r.entity().getItems() == null || r.entity().getItems().isEmpty()) {
                return;
            }

            List<Repository> items = r.entity().getItems();
            int startPosition = repositories.size();
            repositories.addAll(items);
            notifyItemRangeInserted(startPosition, items.size());

            pagedResponse = r.next();
        }
    }
}