package org.kie.uberfire.social.activities.client.widgets.timeline.simple;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.github.gwtbootstrap.client.ui.Fieldset;
import com.github.gwtbootstrap.client.ui.FluidContainer;
import com.github.gwtbootstrap.client.ui.NavList;
import com.github.gwtbootstrap.client.ui.Paragraph;
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Widget;
import org.jboss.errai.bus.client.api.base.MessageBuilder;
import org.jboss.errai.common.client.api.RemoteCallback;
import org.kie.uberfire.social.activities.client.widgets.item.SimpleItemWidget;
import org.kie.uberfire.social.activities.client.widgets.item.model.SimpleItemWidgetModel;
import org.kie.uberfire.social.activities.client.widgets.timeline.simple.model.SimpleSocialTimelineWidgetModel;
import org.kie.uberfire.social.activities.model.PagedSocialQuery;
import org.kie.uberfire.social.activities.model.SocialActivitiesEvent;
import org.kie.uberfire.social.activities.service.SocialTypeTimelinePagedRepositoryAPI;
import org.kie.uberfire.social.activities.service.SocialUserTimelinePagedRepositoryAPI;
import org.uberfire.backend.vfs.Path;
import org.uberfire.backend.vfs.VFSService;

public class SimpleSocialTimelineWidget extends Composite {

    private SimpleSocialTimelineWidgetModel model;

    @UiField
    FluidContainer itemsPanel;

    @UiField
    Fieldset pagination;

    public SimpleSocialTimelineWidget( SimpleSocialTimelineWidgetModel model ) {
        initWidget( uiBinder.createAndBindUi( this ) );
        this.model = model;
        setupPaginationLinks();
        refreshTimelineWidget();
    }

    private void refreshTimelineWidget() {
        itemsPanel.clear();
        createWidgets();
    }

    private void createWidgets() {
        pagination.clear();
        if ( model.isSocialTypeWidget() ) {
            createSociaTypelItemsWidget();
        } else {
            createUserTimelineItemsWidget();
        }
    }

    private void createUserTimelineItemsWidget() {
        MessageBuilder.createCall( new RemoteCallback<PagedSocialQuery>() {
            public void callback( PagedSocialQuery paged ) {
                createTimeline( paged );
            }
        }, SocialUserTimelinePagedRepositoryAPI.class ).getUserTimeline( model.getSocialUser(), model.getSocialPaged(), model.getPredicate() );
    }

    private void createSociaTypelItemsWidget() {
        MessageBuilder.createCall( new RemoteCallback<PagedSocialQuery>() {
            public void callback( PagedSocialQuery paged ) {
                createTimeline( paged );
            }
        }, SocialTypeTimelinePagedRepositoryAPI.class ).getEventTimeline( model.getSocialEventType().name(), model.getSocialPaged(), model.getPredicate() );
    }

    private void createTimeline( PagedSocialQuery paged ) {
        if ( thereIsNoEvents( paged ) ) {
            displayNoEvents();
        } else {
            displayEvents( paged );
        }

    }

    private void displayNoEvents() {
        pagination.add( new Paragraph( "There are no social events...yet!" ) );
    }

    private boolean thereIsNoEvents( PagedSocialQuery paged ) {
        return paged.socialEvents().isEmpty() && !paged.socialPaged().canIGoBackward();
    }

    private void displayEvents( PagedSocialQuery paged ) {
        model.updateSocialPaged( paged.socialPaged() );

        List<SocialActivitiesEvent> events = paged.socialEvents();
        if ( model.isOneTypePerAsset() ) {
            events = clearEventList( events );
        }

        for ( final SocialActivitiesEvent event : events ) {
            if ( event.hasLink() ) {
                createSimpleWidgetWithLink( event );
            } else {
                createSimpleWidget( event );
            }
        }
        setupPaginationButtonsSocial();
    }

    private List<SocialActivitiesEvent> clearEventList( List<SocialActivitiesEvent> events ) {
        List<SocialActivitiesEvent> cleanEvents  = new ArrayList<SocialActivitiesEvent>(  );
        Map<String,Boolean> oneAssetPerTypeMap = new HashMap<String, Boolean>(  );
        for ( SocialActivitiesEvent event : events ) {
            String key = event.getDescription() + event.getType();
            if(!oneAssetPerTypeMap.containsKey( key )){
                oneAssetPerTypeMap.put( key, true );
                cleanEvents.add( event );
            }
        }
        return cleanEvents;
    }

    private void createSimpleWidgetWithLink( final SocialActivitiesEvent event ) {

        final SimpleItemWidgetModel itemModel = new SimpleItemWidgetModel( model, event.getType(),
                                                                           event.getTimestamp(),
                                                                           event.getLinkLabel(),
                                                                           event.getLinkTarget(),
                                                                           event.getLinkType(),
                                                                           event.getAdicionalInfos(),
                                                                           event.getSocialUser() )
                .withLinkCommand( model.getLinkCommand() )
                .withLinkParams( event.getLinkParams() );

        if ( event.isVFSLink() ) {
            MessageBuilder.createCall( new RemoteCallback<Path>() {
                public void callback( Path path ) {
                    itemModel.withLinkPath( path );
                    addItemWidget( itemModel );

                }
            }, VFSService.class ).get( event.getLinkTarget() );
        } else {
            addItemWidget( itemModel );
        }
    }

    private void addItemWidget( SimpleItemWidgetModel model ) {
        SimpleItemWidget item = GWT.create( SimpleItemWidget.class );
        item.init( model );
        itemsPanel.add( item );
    }

    private void createSimpleWidget( SocialActivitiesEvent event ) {
        SimpleItemWidgetModel rowModel = new SimpleItemWidgetModel( event.getType(),
                                                                    event.getTimestamp(),
                                                                    event.getDescription(),
                                                                    event.getAdicionalInfos(),
                                                                    event.getSocialUser() )
                .withLinkParams( event.getLinkParams() );
        addItemWidget( rowModel );
    }

    private void setupPaginationButtonsSocial() {
        NavList list = GWT.create( NavList.class );
        if ( canICreateLessLink() ) {
            list.add( model.getLess() );
        }
        if ( canICreateMoreLink() ) {
            list.add( model.getMore() );
        }
        if ( canICreateLessLink() || canICreateMoreLink() ) {
            pagination.add( list );
        }
    }

    private boolean canICreateMoreLink() {
        return model.getSocialPaged().canIGoForward() && model.getMore() != null;
    }

    private boolean canICreateLessLink() {
        return model.getSocialPaged().canIGoBackward() && model.getLess() != null;
    }

    private void setupPaginationLinks() {
        if ( model.getLess() != null ) {
            createLessLink();
        }
        if ( model.getMore() != null ) {
            createMoreLink();
        }
    }

    private void createMoreLink() {
        model.getMore().addClickHandler( new ClickHandler() {
            @Override
            public void onClick( ClickEvent event ) {
                model.getSocialPaged().forward();
                createWidgets();
            }
        } );
    }

    private void createLessLink() {
        model.getLess().addClickHandler( new ClickHandler() {
            @Override
            public void onClick( ClickEvent event ) {
                model.getSocialPaged().backward();
                refreshTimelineWidget();
            }
        } );
    }

    interface MyUiBinder extends UiBinder<Widget, SimpleSocialTimelineWidget> {

    }

    static MyUiBinder uiBinder = GWT.create( MyUiBinder.class );

}
