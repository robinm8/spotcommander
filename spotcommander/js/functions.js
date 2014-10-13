/*

Copyright 2014 Ole Jon Bjørkum

This file is part of SpotCommander.

SpotCommander is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

SpotCommander is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with SpotCommander.  If not, see <http://www.gnu.org/licenses/>.

*/

// Activities

function showActivity()
{
	var a = getActivity();

	activityLoading();

	xhr_activity = $.get(a.activity+'.php?'+a.subactivity+'&'+a.args, function(xhr_data)
	{
		clearTimeout(timeout_activity_loading);

		hideDiv('div#activity_div');

		setActivityContent(xhr_data);

		setCoverArtSize();
		setCardVerticalSize();
		setEllipsis();

		restoreScrollPosition();

		fadeInDiv('div#activity_div');

		activityLoaded();

		if(ua_is_ios && ua_is_standalone)
		{
			var cookie = { id: 'current_activity_'+project_version, value: JSON.stringify({ activity: a.activity, subactivity: a.subactivity, args: a.args, scroll_position: 0 }), expires: 1 };
			$.cookie(cookie.id, cookie.value, { expires: cookie.expires });
		}
	}).fail(function()
	{
		timeout_activity_error = setTimeout(function()
		{
			hideDiv('div#activity_div');

			setActivityTitle('Error');
			setActivityActions('<div title="Retry" class="actions_div" data-actions="reload_activity" data-highlightclass="darker_grey_highlight" onclick="void(0)"><div class="img_div img_32_div reload_32_img_div"></div></div>');
			setActivityActionsVisibility('visible');

			var append = (ua_is_android_app) ? ' Long-press the back button on your device to go back to the list of computers.' : '';

			setActivityContent('<div id="activity_inner_div"><div id="activity_message_div"><div><div class="img_div img_64_div information_64_img_div"></div></div><div>Request failed. Make sure you are connected. Tap the top right icon to retry.'+append+'</div></div></div>');

			fadeInDiv('div#activity_div');
		}, 1000);
	});
}

function activityLoading()
{
	xhr_activity.abort();

	hideActivityOverflowActions();
	hideMenu();
	hideNowplayingOverflowActions();
	hideNowplaying();

	hideDiv('div#activity_div');

	scroll_position_save_disable = true;

	scrollToTop(false);

	clearTimeout(timeout_scroll_position_save_disable);

	timeout_scroll_position_save_disable = setTimeout(function()
	{
		scroll_position_save_disable = false;
	}, 500);

	clearTimeout(timeout_activity_loading);

	timeout_activity_loading = setTimeout(function()
	{
		setActivityActionsVisibility('visible');
		setActivityActions('<div><div class="img_div img_32_div loading_32_img_div"></div></div>');
		setActivityTitle('Wait...');

		$('div#activity_div').empty();
	}, 1000);
}

function activityLoaded()
{
	// All
	clearTimeout(timeout_activity_error);

	checkForDialogs();
	checkForUpdates('auto');

	var data = getActivityData();

	if(typeof data.project_version != 'undefined') checkIfNewVersionIsInstalled(data.project_version);

	var title = (typeof data.title == 'undefined') ? 'Unknown' : data.title;

	setActivityTitle(title);

	if(typeof data.actions == 'undefined')
	{
		setActivityActionsVisibility('hidden');
	}
	else
	{
		if(data.actions.length == 1)
		{
			setActivityActions('<div title="'+data.actions[0].action[0]+'" class="actions_div" data-highlightclass="darker_grey_highlight" onclick="void(0)"><div class="img_div img_32_div '+data.actions[0].action[1]+'"></div></div>');

			for(var i = 0; i < data.actions[0].keys.length; i++)
			{
				$('div.actions_div', 'div#top_actionbar_inner_right_div > div').data(data.actions[0].keys[i], data.actions[0].values[i]);
			}
		}
		else
		{
			setActivityActions('<div title="More" class="actions_div" data-actions="show_activity_overflow_actions" data-highlightclass="darker_grey_highlight" onclick="void(0)"><div class="img_div img_32_div overflow_32_img_div"></div></div>');

			$('div#top_actionbar_overflow_actions_inner_div').empty();

			for(var i = 0; i < data.actions.length; i++)
			{
				var highlight_arrow = (i == 0) ? 'data-highlightotherelement="div#top_actionbar_overflow_actions_arrow_div" data-highlightotherelementparent="div#top_actionbar_overflow_actions_div" data-highlightotherelementclass="up_arrow_dark_grey_highlight"' : '';

				$('div#top_actionbar_overflow_actions_inner_div').append('<div class="actions_div" data-highlightclass="dark_grey_highlight" '+highlight_arrow+' onclick="void(0)">'+data.actions[i].action[0]+'</div>');

				for(var f = 0; f < data.actions[i].keys.length; f++)
				{
					$('div.actions_div', 'div#top_actionbar_overflow_actions_inner_div').last().data(data.actions[i].keys[f], data.actions[i].values[f]);
				}
			}
		}

		setActivityActionsVisibility('visible');
	}

	showMenuIndicator();

	// Cover art
	if(typeof data.cover_art_uri != 'undefined' && data.cover_art_uri != '') getCoverArt(data.cover_art_uri);

	// Activities
	if(isActivity('playlists', '') && typeof data.is_authorized_with_spotify != 'undefined')
	{
		if(data.is_authorized_with_spotify)
		{
			var cookie = { id: 'last_playlists_import' };

			if(isCookie(cookie.id))
			{
				var last_playlists_import = parseInt($.cookie(cookie.id));

				if(getCurrentTime() - last_playlists_import > 1000 * 300) importSpotifyPlaylists(true);
			}
			else
			{
				showToast('Getting your playlists...', 2);

				importSpotifyPlaylists(true);
			}
		}
	}
	else if(isActivity('library', ''))
	{
		if(data.is_authorized_with_spotify)
		{
			var cookie = { id: 'last_saved_tracks_import' };

			if(isCookie(cookie.id))
			{
				var last_saved_tracks_import = parseInt($.cookie(cookie.id));

				if(getCurrentTime() - last_saved_tracks_import > 1000 * 300) importSavedSpotifyTracks(true);
			}
			else
			{
				showToast('Getting your saved tracks...', 2);

				importSavedSpotifyTracks(true);
			}
		}
	}
	else if(isActivity('browse', ''))
	{
		if(ua_supports_csstransitions && ua_supports_csstransforms3d)
		{
			var last_index = $('div.card_div').length - 1;

			setTimeout(function()
			{
				$('div.card_div').addClass('prepare_browse_card_animation').each(function(index)
				{
					var element = $(this);
					var timeout = index * 75;

					setTimeout(function()
					{
						element.addClass('show_browse_card_animation');

						if(index == last_index)
						{
							element.one(event_transitionend, function()
							{
								fadeInDiv('div#browse_featured_playlists_div > div');
							});
						}
					}, timeout);
				});

				getFeaturedPlaylists(false);
			}, 250);
		}
		else
		{
			showDiv('div.card_div');
			getFeaturedPlaylists(true);
		}
	}
	else if(isActivity('settings', ''))
	{
		if(ua_is_android_app) $('div#settings_android_app_div').show();
		if(!ua_supports_notifications) disableSetting('div.setting_notifications_div');
	}

	// Text fields
	if(!ua_supports_touch)
	{
		if($('input:text#create_playlist_name_input').length)
		{
			focusTextInput('input:text#create_playlist_name_input');
		}
		else if($('input:text#edit_playlist_name_input').length)
		{
			var element = $('input:text#edit_playlist_name_input');
			var length = element.val().length;

			focusTextInput(element);

			element[0].setSelectionRange(length, length);
		}
		else if($('input:text#search_input').length)
		{
			focusTextInput('input:text#search_input');
		}
	}
}

function changeActivity(activity, subactivity, args)
{
	var args = args.replace(/%26/g, '%2526').replace(/&amp;/g, '&').replace(/%2F/g, '%252F').replace(/%5C/g, '%255C');
	var hash  = '#'+activity+'/'+subactivity+'/'+args+'/'+getCurrentTime();

	window.location.href=hash;
}

function changeActivityIfIsAuthorizedWithSpotify(activity, subactivity, args, is_authorized_with_spotify)
{
	if(is_authorized_with_spotify)
	{
		changeActivity(activity, subactivity, args);
	}
	else
	{
		changeActivity('profile', '', '');
	}
}

function replaceActivity(activity, subactivity, args)
{
	var args = args.replace(/%26/g, '%2526').replace(/&amp;/g, '&').replace(/%2F/g, '%252F').replace(/%5C/g, '%255C');
	var hash  = '#'+activity+'/'+subactivity+'/'+args+'/'+getCurrentTime();

	window.location.replace(hash);
}

function reloadActivity()
{
	var a = getActivity();
	replaceActivity(a.activity, a.subactivity, a.args);
}

function refreshActivity()
{
	var a = getActivity();

	$.get(a.activity+'.php?'+a.subactivity+'&'+a.args, function(xhr_data)
	{
		if(isActivityWithArgs(a.activity, a.subactivity, a.args))
		{
			setActivityContent(xhr_data);

			setCoverArtSize();
			setCardVerticalSize();
			setEllipsis();
		}
	});
}

function getActivity()
{
	var hash = window.location.hash.slice(1);

	if(hash == '')
	{
		var a = getDefaultActivity();
		var activity = [a.activity, a.subactivity, a.args, a.time];
	}
	else
	{
		var activity = hash.split('/');
	}

	return { activity: activity[0], subactivity: activity[1], args: activity[2], time: activity[3] };
}

function getActivityData()
{
	return ($('div#activity_inner_div').length && $('div#activity_inner_div').attr('data-activitydata')) ? $.parseJSON($.base64.decode($('div#activity_inner_div').data('activitydata'))) : '';
}

function getDefaultActivity()
{
	var cookie = { id: 'hide_first_time_activity', value: 'true', expires: 3650 };

	if(!isCookie(cookie.id))
	{
		var activity = { activity: 'first-time', subactivity: '', args: '', time: 'default' };
		$.cookie(cookie.id, cookie.value, { expires: cookie.expires });
	}
	else
	{
		var activity = { activity: 'playlists', subactivity: '', args: '', time: 'default' };
	}

	return activity;
}

function setActivityTitle(title)
{
	$('div#top_actionbar_inner_center_div > div').attr('title', title).html(title);
}

function setActivityActions(actions)
{
	$('div#top_actionbar_inner_right_div > div').html(actions);
}

function setActivityActionsVisibility(visibility)
{
	$('div#top_actionbar_inner_right_div > div').css('visibility', visibility);
}

function setActivityContent(content)
{
	$('div#activity_div').html(content);
}

function isActivity(activity, subactivity)
{
	var a = getActivity();
	return (a.activity == activity && a.subactivity == subactivity);
}

function isActivityWithArgs(activity, subactivity, args)
{
	var a = getActivity();
	return (a.activity == activity && a.subactivity == subactivity && a.args == args);
}

function goBack()
{
	if(ua_is_ios && ua_is_standalone)
	{
		if(!isDisplayed('div#transparent_cover_div') && !isDisplayed('div#black_cover_div') && !isDisplayed('div#black_cover_activity_div') && !isVisible('div#nowplaying_div') && !textInputHasFocus()) history.back();
	}
	else
	{
		if(isDisplayed('div#dialog_div'))
		{
			closeDialog();
		}
		else if(isDisplayed('div#top_actionbar_overflow_actions_div'))
		{
			hideActivityOverflowActions();
		}
		else if(isDisplayed('div#nowplaying_actionbar_overflow_actions_div'))
		{
			hideNowplayingOverflowActions();
		}
		else if(menuIsVisible())
		{
			hideMenu();
		}
		else if(isVisible('div#nowplaying_div'))
		{
			hideNowplaying();
		}
		else
		{
			history.back();
		}
	}
}

function openExternalActivity(uri)
{
	if(ua_is_android_app)
	{
		if(shc(uri, 'https://www.youtube.com/results?search_query='))
		{
			var query = decodeURIComponent(uri.replace('https://www.youtube.com/results?search_query=', ''));
			var query = Android.JSsearchApp('com.google.android.youtube', query);

			if(query == 0) showToast('App not installed', 4);
		}
		else
		{
			Android.JSopenUri(uri);
		}
	}
	else
	{
		if(ua_is_android && shc(ua, 'Android 2') || ua_is_ios && ua_is_standalone)
		{
			var a = document.createElement('a');
			a.setAttribute('href', uri);
			a.setAttribute('target', '_blank');
			var dispatch = document.createEvent('HTMLEvents');
			dispatch.initEvent('click', true, true);
			a.dispatchEvent(dispatch);
		}
		else
		{
			window.open(uri);
		}
	}
}

// Menus

function toggleMenu()
{
	if(menuIsVisible())
	{
		hideMenu();
	}
	else
	{
		showMenu();
	}
}

function showMenu()
{
	if(menuIsVisible() || isDisplayed('div#transparent_cover_div') || isDisplayed('div#black_cover_div') || isDisplayed('div#black_cover_activity_div') || isVisible('div#nowplaying_div') || textInputHasFocus()) return;

	$('div#menu_div').css('visibility', 'visible');
	$('div#black_cover_activity_div').show();

	setTimeout(function()
	{
		if(ua_supports_csstransitions && ua_supports_csstransforms3d)
		{
			$('div#menu_div').addClass('show_menu_animation');
			$('div#top_actionbar_inner_left_div > div > div > div').addClass('show_menu_img_animation');
			$('div#black_cover_activity_div').addClass('show_black_cover_activity_div_animation');
		}
		else
		{
			$('div#menu_div').stop().animate({ left: '0' }, 250, 'easeOutExpo');
			$('div#black_cover_activity_div').stop().animate({ 'opacity': '0.5' }, 250, 'easeOutExpo');
		}
	}, 25);

	nativeAppCanCloseCover();
}

function hideMenu()
{
	if(window_width >= 1024 || !menuIsVisible() || isDisplayed('div#dialog_div')) return;

	if(ua_supports_csstransitions && ua_supports_csstransforms3d)
	{
		$('div#menu_div').addClass('hide_menu_animation').one(event_transitionend, function()
		{
			$('div#menu_div').css('visibility', '').removeClass('show_menu_animation hide_menu_animation');
			nativeAppCanCloseCover();
		});

		$('div#top_actionbar_inner_left_div > div > div > div').addClass('hide_menu_img_animation').one(event_transitionend, function()
		{
			$('div#top_actionbar_inner_left_div > div > div > div').removeClass('show_menu_img_animation hide_menu_img_animation');
		});

		$('div#black_cover_activity_div').addClass('hide_black_cover_activity_div_animation').one(event_transitionend, function()
		{
			$('div#black_cover_activity_div').hide().removeClass('show_black_cover_activity_div_animation hide_black_cover_activity_div_animation');
		});
	}
	else
	{
		$('div#menu_div').stop().animate({ left: $('div#menu_div').data('cssleft') }, 250, 'easeOutExpo', function()
		{
			$('div#menu_div').css('visibility', '').css('left', '');
			nativeAppCanCloseCover();
		});

		$('div#black_cover_activity_div').stop().animate({ 'opacity': '0' }, 250, 'easeOutExpo', function()
		{
			$('div#black_cover_activity_div').hide().css('opacity', '');
		});
	}
}

function showMenuIndicator()
{
	$('div.menu_big_item_indicator_div').removeClass('menu_big_item_indicator_active_div');
	$('div.menu_big_item_text_div').css('font-weight', '');

	var a = getActivity();
	var current_activity = a.activity;

	$('div.menu_big_item_div').each(function()
	{
		var element = $(this);
		var activity = element.data('activity');

		if(activity == current_activity)
		{
			$('div.menu_big_item_indicator_div', element).addClass('menu_big_item_indicator_active_div');
			$('div.menu_big_item_text_div', element).css('font-weight', 'bold');
		}
	});
}

function toggleActivityOverflowActions()
{
	if(isDisplayed('div#top_actionbar_overflow_actions_div'))
	{
		hideActivityOverflowActions();
	}
	else
	{
		showActivityOverflowActions();
	}
}

function showActivityOverflowActions()
{
	if(isDisplayed('div#top_actionbar_overflow_actions_div')) return;

	hideMenu();

	$('div#transparent_cover_div').show();
	$('div#top_actionbar_overflow_actions_div').show();

	if(ua_supports_csstransitions && ua_supports_csstransforms3d)
	{
		$('div#top_actionbar_overflow_actions_div').addClass('prepare_overflow_actions_animation');

		setTimeout(function()
		{		
			$('div#top_actionbar_overflow_actions_div').addClass('show_overflow_actions_animation');
		}, 25);
	}
	else
	{
		fadeInDiv('div#top_actionbar_overflow_actions_div');
	}

	nativeAppCanCloseCover();
}

function hideActivityOverflowActions()
{
	if(!isDisplayed('div#top_actionbar_overflow_actions_div')) return;

	$('div#top_actionbar_overflow_actions_div').hide();
	$('div#top_actionbar_overflow_actions_div div').removeClass('up_arrow_dark_grey_highlight dark_grey_highlight');

	if(ua_supports_csstransitions && ua_supports_csstransforms3d)
	{
		$('div#top_actionbar_overflow_actions_div').removeClass('prepare_overflow_actions_animation show_overflow_actions_animation');
	}
	else
	{
		hideDiv('div#top_actionbar_overflow_actions_div');
	}

	$('div#transparent_cover_div').hide();

	nativeAppCanCloseCover();
}

// Remote control

function remoteControl(action)
{
	xhr_remote_control.abort();

	clearTimeout(timeout_remote_control);

	if(action == 'launch_quit' || action == 'next' || action == 'previous') startRefreshNowplaying();

	xhr_remote_control = $.post('main.php?'+getCurrentTime(), { action: action }, function(xhr_data)
	{
		if(action == 'launch_quit')
		{
			refreshNowplaying('manual');
		}
		else if(action == 'play_pause' || action == 'pause')
		{
			refreshNowplaying('silent');
		}
		else if(action == 'next' || action == 'previous')
		{
			var timeout = (xhr_data == 'queue_is_empty') ? 500 : 1000;

			timeout_remote_control = setTimeout(function()
			{
				refreshNowplaying('manual');
			}, timeout);
		}
	});
}

function adjustVolume(volume)
{
	xhr_adjust_volume.abort();

	autoRefreshNowplaying('reset');

	var cookie = { id: 'settings_volume_control' };
	var control = $.cookie(cookie.id);
	var action = (control == 'spotify') ? 'adjust_spotify_volume' : 'adjust_system_volume';

	xhr_adjust_volume = $.post('main.php?'+getCurrentTime(), { action: action, data: volume }, function(xhr_data)
	{
		$('input#nowplaying_volume_slider').val(xhr_data);
		$('span#nowplaying_volume_level_span').html(xhr_data);

		if(!isVisible('#nowplaying_div'))
		{
			if(xhr_data == 0)
			{
				showToast('Volume muted', 1);
			}
			else
			{
				showToast('Volume: '+xhr_data+' %', 1);
			}
		}
	});
}

function adjustVolumeControl(control)
{
	var cookie = { id: 'hide_adjust_volume_control_dialog_'+project_version, value: 'true', expires: 3650 };

	if(!isCookie(cookie.id))
	{
		showDialog({ title: 'Adjust Volume', body_class: 'dialog_message_div', body_text: 'With this action you can toggle between adjusting Spotify\'s volume and the system volume.<br><br>Continue?', button1: { text: 'No', keys : ['actions'], values: ['hide_dialog'] }, button2: { text: 'Yes', keys : ['actions', 'cookieid', 'cookievalue', 'cookieexpires', 'volumecontrol'], values: ['hide_dialog set_cookie adjust_volume_control', cookie.id, cookie.value, cookie.expires, control] } });
	}
	else
	{
		var cookie = { id: 'settings_volume_control', value: control, expires: 3650 };
		$.cookie(cookie.id, cookie.value, { expires: cookie.expires });

		if(control == 'spotify')
		{
			showToast('Controlling Spotify\'s volume', 2);
		}
		else
		{
			showToast('Controlling the system volume', 2);
		}

		refreshNowplaying('silent');
	}
}

function toggleShuffleRepeat(action)
{
	var cookie = { id: 'hide_shuffle_repeat_dialog_'+project_version, value: 'true', expires: 3650 };

	if(!isCookie(cookie.id))
	{
		showDialog({ title: 'Shuffle &amp; Repeat', body_class: 'dialog_message_div', body_text: 'Shuffle and repeat can be toggled, but it is not possible to get the current status. Spotify must not be minimized to tray. Advertisements may stop this from working.<br><br>Continue?', button1: { text: 'No', keys : ['actions'], values: ['hide_dialog'] }, button2: { text: 'Yes', keys : ['actions', 'cookieid', 'cookievalue', 'cookieexpires', 'remotecontrol'], values: ['hide_dialog set_cookie toggle_shuffle_repeat', cookie.id, cookie.value, cookie.expires, action] } });
	}
	else
	{
		$.post('main.php?'+getCurrentTime(), { action: action }, function(xhr_data)
		{
			if(xhr_data == 'spotify_is_not_running')
			{
				showToast('Spotify is not running', 2);
			}
			else if(action == 'toggle_shuffle')
			{
				showToast('Shuffle toggled', 2);
			}
			else if(action == 'toggle_repeat')
			{
				showToast('Repeat toggled', 2);
			}		
		});
	}
}

function playUri(uri)
{
	startRefreshNowplaying();

	$.post('main.php?'+getCurrentTime(), { action: 'play_uri', data: uri }, function()
	{
		refreshNowplaying('manual');
	});

	if(getUriType(uri) == 'playlist') saveRecentPlaylist(uri);
}

function shufflePlayUri(uri)
{
	var cookie = { id: 'hide_shuffle_play_uri_dialog_'+project_version, value: 'true', expires: 3650 };

	if(!isCookie(cookie.id))
	{
		showDialog({ title: 'Shuffle Play', body_class: 'dialog_message_div', body_text: 'This action plays the media, toggles shuffle off/on and skips one track to ensure random playback. Shuffle must already be enabled. Spotify must not be minimized to tray. Advertisements may stop this from working.<br><br>Continue?', button1: { text: 'No', keys : ['actions'], values: ['hide_dialog'] }, button2: { text: 'Yes', keys : ['actions', 'cookieid', 'cookievalue', 'cookieexpires', 'uri'], values: ['hide_dialog set_cookie shuffle_play_uri', cookie.id, cookie.value, cookie.expires, uri] } });
	}
	else
	{
		startRefreshNowplaying();

		$.post('main.php?'+getCurrentTime(), { action: 'shuffle_play_uri', data: uri }, function()
		{
			refreshNowplaying('manual');
		});

		if(getUriType(uri) == 'playlist') saveRecentPlaylist(uri);
	}
}

function startTrackRadio(uri, play_first)
{
	var cookie = { id: 'hide_start_track_radio_dialog_'+project_version, value: 'true', expires: 3650 };

	if(getUriType(uri) == 'local')
	{
		showToast('Not possible for local files', 4);
	}
	else if(!isCookie(cookie.id))
	{
		showDialog({ title: 'Start Track Radio', body_class: 'dialog_message_div', body_text: 'Spotify must not be minimized to tray. If the up keyboard key is simulated wrong number of times, change it in settings. It may not work on all tracks. Advertisements may stop this from working.<br><br>Continue?', button1: { text: 'No', keys : ['actions'], values: ['hide_dialog'] }, button2: { text: 'Yes', keys: ['actions', 'cookieid', 'cookievalue', 'cookieexpires', 'uri', 'playfirst'], values: ['hide_dialog set_cookie start_track_radio', cookie.id, cookie.value, cookie.expires, uri, play_first] } });
	}
	else
	{
		showToast('Starting track radio', 2);

		var data = JSON.stringify([uri, play_first, settings_start_track_radio_simulation]);

		$.post('main.php?'+getCurrentTime(), { action: 'start_track_radio', data: data }, function()
		{
			startRefreshNowplaying();

			setTimeout(function()
			{
				refreshNowplaying('manual');
			}, 2000);
		});
	}
}

function confirmSuspendComputer()
{
	showDialog({ title: 'Suspend Computer', body_class: 'dialog_message_div', body_text: 'This will suspend the computer running Spotify, and you will lose connection to '+project_name+'.<br><br>Continue?', button1: { text: 'No', keys : ['actions'], values: ['hide_dialog'] }, button2: { text: 'Yes', keys : ['actions'], values: ['hide_dialog suspend_computer'] } });
}

function suspendComputer()
{
	remoteControl('suspend_computer');

	if(ua_is_android_app) Android.JSfinishActivity();
}

function confirmShutDownComputer()
{
	showDialog({ title: 'Shut Down Computer', body_class: 'dialog_message_div', body_text: 'This will shut down the computer running Spotify, and you will lose connection to '+project_name+'. ConsoleKit must be installed for this to work.<br><br>Continue?', button1: { text: 'No', keys : ['actions'], values: ['hide_dialog'] }, button2: { text: 'Yes', keys : ['actions'], values: ['hide_dialog shut_down_computer'] } });
}

function shutDownComputer()
{
	remoteControl('shut_down_computer');

	if(ua_is_android_app) Android.JSfinishActivity();
}

// Now playing

function toggleNowplaying()
{
	if(isVisible('div#nowplaying_div'))
	{
		hideNowplayingOverflowActions();
		hideNowplaying();
	}
	else
	{
		hideActivityOverflowActions();
		showNowplaying();
	}
}

function showNowplaying()
{
	if(isVisible('div#nowplaying_div') || textInputHasFocus()) return;

	hideMenu();

	$('div#nowplaying_div').css('visibility', 'visible');

	if(ua_supports_csstransitions && ua_supports_csstransforms3d)
	{
		$('div#nowplaying_div').addClass('show_nowplaying_animation');
	}
	else
	{
		$('div#nowplaying_div').stop().animate({ bottom: '0' }, 500, 'easeOutExpo');
	}

	nativeAppCanCloseCover();
}

function hideNowplaying()
{
	if(!isVisible('div#nowplaying_div') || isDisplayed('div#transparent_cover_div')) return;

	if(ua_supports_csstransitions && ua_supports_csstransforms3d)
	{
		$('div#nowplaying_div').addClass('hide_nowplaying_animation').one(event_transitionend, function()
		{
			$('div#nowplaying_div').css('visibility', '').removeClass('show_nowplaying_animation hide_nowplaying_animation');
			nativeAppCanCloseCover();
		});
	}
	else
	{
		$('div#nowplaying_div').stop().animate({ bottom: $('div#nowplaying_div').data('cssbottom') }, 500, 'easeOutExpo', function()
		{
			$('div#nowplaying_div').css('visibility', '');
			nativeAppCanCloseCover();
		});
	}
}

function toggleNowplayingOverflowActions()
{
	if(isDisplayed('div#nowplaying_actionbar_overflow_actions_div'))
	{
		hideNowplayingOverflowActions();
	}
	else
	{
		showNowplayingOverflowActions();
	}
}

function showNowplayingOverflowActions()
{
	if(isDisplayed('div#nowplaying_actionbar_overflow_actions_div')) return;

	$('div#transparent_cover_div').show();
	$('div#nowplaying_actionbar_overflow_actions_div').show();

	if(ua_supports_csstransitions && ua_supports_csstransforms3d)
	{
		$('div#nowplaying_actionbar_overflow_actions_div').addClass('prepare_overflow_actions_animation');

		setTimeout(function()
		{		
			$('div#nowplaying_actionbar_overflow_actions_div').addClass('show_overflow_actions_animation');
		}, 25);
	}
	else
	{
		fadeInDiv('div#nowplaying_actionbar_overflow_actions_div');
	}

	$('input#nowplaying_volume_slider').attr('disabled', 'disabled');

	nativeAppCanCloseCover();
}

function hideNowplayingOverflowActions()
{
	if(!isDisplayed('div#nowplaying_actionbar_overflow_actions_div')) return;

	$('div#nowplaying_actionbar_overflow_actions_div').hide();
	$('div#nowplaying_actionbar_overflow_actions_div div').removeClass('up_arrow_dark_grey_highlight dark_grey_highlight');

	if(ua_supports_csstransitions && ua_supports_csstransforms3d)
	{
		$('div#nowplaying_actionbar_overflow_actions_div').removeClass('prepare_overflow_actions_animation show_overflow_actions_animation');
	}
	else
	{
		hideDiv('div#nowplaying_actionbar_overflow_actions_div');
	}

	$('div#transparent_cover_div').hide();

	setTimeout(function()
	{
		$('input#nowplaying_volume_slider').removeAttr('disabled');
	}, 250);

	nativeAppCanCloseCover();
}

function startRefreshNowplaying()
{
	nowplaying_refreshing = true;

	xhr_nowplaying.abort();

	clearTimeout(timeout_nowplaying_error);

	$('div#bottom_actionbar_inner_center_div > div').html('Refreshing...');

	if(!isVisible('div#nowplaying_div')) return;

	hideNowplayingOverflowActions();

	$('div#nowplaying_actionbar_right_div > div').removeClass('actions_div').css('opacity', '0.5');

	if(ua_supports_csstransitions && ua_supports_csstransforms3d)
	{
		$('div#nowplaying_cover_art_div').css('transition', '').css('transform', '').css('-webkit-transition', '').css('-webkit-transform', '').css('-moz-transition', '').css('-moz-transform', '').css('-ms-transition', '').css('-ms-transform', '');
		$('div#nowplaying_cover_art_div').off(event_transitionend).removeClass('prepare_nowplaying_cover_art_animation show_nowplaying_cover_art_animation hide_nowplaying_cover_art_animation').addClass('hide_nowplaying_cover_art_animation');
	}
	else
	{
		$('div#nowplaying_cover_art_div').stop().animate({ left: '-'+window_width+'px' }, 500, 'easeOutExpo');
	}
}

function refreshNowplaying(type)
{
	nowplaying_refreshing = true;

	xhr_nowplaying.abort();

	autoRefreshNowplaying('reset');

	clearTimeout(timeout_nowplaying_error);

	xhr_nowplaying = $.get('nowplaying.php', function(xhr_data)
	{
		nowplaying_refreshing = false;

		clearTimeout(timeout_nowplaying_error);

		if(type == 'manual' || nowplaying_last_data != xhr_data)
		{
			nowplaying_last_data = xhr_data;

			var nowplaying = $.parseJSON(xhr_data);

			checkIfNewVersionIsInstalled(nowplaying.project_version);

			$('div#nowplaying_actionbar_right_div > div').addClass('actions_div').data('actions', 'show_nowplaying_overflow_actions').css('opacity', '');

			$('div#nowplaying_actionbar_overflow_actions_inner_div').empty();

			for(var i = 0; i < nowplaying.actions.length; i++)
			{
				var highlight_arrow = (i == 0) ? 'data-highlightotherelement="div#nowplaying_actionbar_overflow_actions_arrow_div" data-highlightotherelementparent="div#nowplaying_actionbar_overflow_actions_div" data-highlightotherelementclass="up_arrow_dark_grey_highlight"' : '';

				$('div#nowplaying_actionbar_overflow_actions_inner_div').append('<div class="actions_div" data-highlightclass="dark_grey_highlight" '+highlight_arrow+' onclick="void(0)">'+nowplaying.actions[i].action[0]+'</div>');

				for(var f = 0; f < nowplaying.actions[i].keys.length; f++)
				{
					$('div.actions_div', 'div#nowplaying_actionbar_overflow_actions_inner_div').last().data(nowplaying.actions[i].keys[f], nowplaying.actions[i].values[f]);
				}
			}

			$('div#nowplaying_artist_div').attr('title', nowplaying.artist).html(hsc(nowplaying.artist));
			$('div#nowplaying_title_div').attr('title', nowplaying.title+' ('+nowplaying.tracklength+')').html(hsc(nowplaying.title));

			$('input#nowplaying_volume_slider').val(nowplaying.current_volume);
			$('span#nowplaying_volume_level_span').html(nowplaying.current_volume);

			$('div#nowplaying_play_pause_div').removeClass('play_64_img_div pause_64_img_div').addClass(nowplaying.play_pause+'_64_img_div');

			$('div#bottom_actionbar_inner_left_div > div > div > div').removeClass('play_32_img_div pause_32_img_div').addClass(nowplaying.play_pause+'_32_img_div');

			if(type == 'manual' || nowplaying_last_uri != nowplaying.uri)
			{
				nowplaying_last_uri = nowplaying.uri;

				$.cookie('nowplaying_uri', nowplaying.uri.toLowerCase(), { expires: 3650 });

				$('div#nowplaying_cover_art_div').data('uri', nowplaying.uri).attr('title', nowplaying.album+' ('+nowplaying.released+')');

				if(nowplaying.is_local && nowplaying.artist != 'Unknown' && nowplaying.album != 'Unknown')
				{
					$.getJSON(project_website+'api/1/cover-art/?type=album&artist='+encodeURIComponent(nowplaying.artist)+'&album='+encodeURIComponent(nowplaying.album)+'&callback=?', function(json_data)
					{
						var metadata = json_data;
						var lastfm_cover_art = (typeof metadata.mega == 'undefined' || metadata.mega == '') ? 'img/no-cover-art-640.png?'+project_serial : metadata.mega;

						$('img#nowplaying_cover_art_preload_img').attr('src', 'img/album-24.png?'+project_serial).attr('src', lastfm_cover_art).on('load error', function(event)
						{
							$(this).off('load error');

							var cover_art = (event.type == 'load') ? lastfm_cover_art : 'img/no-cover-art-640.png?'+project_serial;
							$('div#nowplaying_cover_art_div').css('background-image', 'url("'+cover_art+'")');

							if(type == 'manual') endRefreshNowplaying();

							if(nowplaying.uri != '') showNotification(nowplaying.title, nowplaying.artist+' (click to skip)', cover_art, 'remote_control_next', 4);
						});
					});
				}
				else
				{
					$('img#nowplaying_cover_art_preload_img').attr('src', 'img/album-24.png?'+project_serial).attr('src', nowplaying.cover_art).on('load error', function(event)
					{
						$(this).off('load error');

						var cover_art = (event.type == 'load') ? nowplaying.cover_art : 'img/no-cover-art-640.png?'+project_serial;
						$('div#nowplaying_cover_art_div').css('background-image', 'url("'+cover_art+'")');

						if(type == 'manual') endRefreshNowplaying();

						if(nowplaying.uri != '') showNotification(nowplaying.title, nowplaying.artist+' (click to skip)', cover_art, 'remote_control_next', 4);
					});
				}

				hideDiv('div#bottom_actionbar_inner_center_div > div');
				$('div#bottom_actionbar_inner_center_div > div').attr('title', nowplaying.artist+' - '+nowplaying.title+' ('+nowplaying.tracklength+')').html(hsc(nowplaying.title));
				fadeInDiv('div#bottom_actionbar_inner_center_div > div');

				highlightNowplayingListItem();
				refreshRecentlyPlayedActivity();
				refreshQueueActivity();

				if(settings_update_lyrics && isActivity('lyrics', '') && nowplaying.uri != '') replaceActivity('lyrics', '', 'artist='+encodeURIComponent(nowplaying.artist)+'&title='+encodeURIComponent(nowplaying.title));

				if(ua_is_android_app)
				{
					Android.JSsetSharedString('NOWPLAYING_ARTIST', nowplaying.artist);
					Android.JSsetSharedString('NOWPLAYING_TITLE', nowplaying.title);
				}
				else if(integrated_in_msie)
				{
					if(nowplaying.play_pause == 'play')
					{
						window.external.msSiteModeShowButtonStyle(ie_thumbnail_button_play_pause, ie_thumbnail_button_style_play);
					}
					else
					{
						window.external.msSiteModeShowButtonStyle(ie_thumbnail_button_play_pause, ie_thumbnail_button_style_pause);
					}
				}
			}
			else
			{
				$('div#bottom_actionbar_inner_center_div > div').attr('title', nowplaying.artist+' - '+nowplaying.title+' ('+nowplaying.tracklength+')').html(hsc(nowplaying.title));
			}
		}
	}).fail(function()
	{
		timeout_nowplaying_error = setTimeout(function()
		{
			nowplaying_refreshing = false;
			nowplaying_last_data = '';
			nowplaying_last_uri = '';

			$('div#bottom_actionbar_inner_center_div > div').html('Connection failed');
		}, 1000);
	});
}

function endRefreshNowplaying()
{
	if(ua_supports_csstransitions && ua_supports_csstransforms3d)
	{
		if(isVisible('div#nowplaying_div'))
		{
			$('div#nowplaying_cover_art_div').removeClass('hide_nowplaying_cover_art_animation').addClass('prepare_nowplaying_cover_art_animation');

			setTimeout(function()
			{
				$('div#nowplaying_cover_art_div').addClass('show_nowplaying_cover_art_animation').one(event_transitionend, function()
				{
					$('div#nowplaying_cover_art_div').removeClass('prepare_nowplaying_cover_art_animation show_nowplaying_cover_art_animation hide_nowplaying_cover_art_animation');
				});
			}, 25);
		}
		else
		{
			$('div#nowplaying_cover_art_div').removeClass('prepare_nowplaying_cover_art_animation show_nowplaying_cover_art_animation hide_nowplaying_cover_art_animation');
		}
	}
	else
	{
		if(isVisible('div#nowplaying_div'))
		{
			var changeside = parseInt(window_width * 2);
			$('div#nowplaying_cover_art_div').stop().css('left', changeside+'px').animate({ left: '0' }, 500, 'easeOutExpo');
		}
		else
		{
			$('div#nowplaying_cover_art_div').css('left', '');
		}
	}
}

function autoRefreshNowplaying(action)
{
	if(action == 'start' && settings_nowplaying_refresh_interval >= 5)
	{
		var cookie = { id: 'nowplaying_last_update', expires: 3650 };
		$.cookie(cookie.id, getCurrentTime(), { expires: cookie.expires });

		interval_nowplaying_auto_refresh = setInterval(function()
		{
			if(getCurrentTime() - parseInt($.cookie(cookie.id)) > settings_nowplaying_refresh_interval * 1000)
			{
				timeout_nowplaying_auto_refresh = setTimeout(function()
				{
					if(!nowplaying_refreshing && !nowplaying_cover_art_moving && !isDisplayed('div#black_cover_div') && !isDisplayed('div#transparent_cover_div')) refreshNowplaying('silent');
				}, 2000);

				$.cookie(cookie.id, getCurrentTime(), { expires: cookie.expires });
			}
		}, 1000);
	}
	else if(action == 'reset' && interval_nowplaying_auto_refresh != null)
	{
		clearInterval(interval_nowplaying_auto_refresh);
		clearTimeout(timeout_nowplaying_auto_refresh);

		autoRefreshNowplaying('start');
	}
}

function highlightNowplayingListItem()
{
	var cookie = { id: 'nowplaying_uri' };

	if(!$('div.list_item_main_div').length || !isCookie(cookie.id)) return;

	var nowplaying_uri = $.cookie(cookie.id);

	$('div.list_item_main_div').each(function()
	{
		var item = $(this);

		if(item.attr('data-trackuri'))
		{
			var uri = item.data('trackuri').toLowerCase();
			var img_div = 'track_24_img_div';
			
			if(getUriType(uri) == 'local')
			{
				img_div = 'local_24_img_div';
			}

			var icon = $('div.list_item_main_inner_icon_div > div', item);
			var text = $('div.list_item_main_inner_text_upper_div', item);

			if(icon.hasClass('playing_24_img_div') && text.hasClass('bold_text'))
			{
				icon.removeClass('playing_24_img_div').addClass(img_div);
				text.removeClass('bold_text');
			}

			if(uri == nowplaying_uri)
			{
				icon.removeClass('track_24_img_div local_24_img_div').addClass('playing_24_img_div');
				text.addClass('bold_text');
			}
		}
	});
}

// Cover art

function getCoverArt(uri)
{
	xhr_cover_art.abort();

	xhr_cover_art = $.post('main.php?'+getCurrentTime(), { action: 'get_cover_art', data: uri }, function(xhr_data)
	{
		if(getUriType(xhr_data) == 'cover_art')
		{
			$('img#cover_art_preload_img').attr('src', 'img/album-24.png?'+project_serial).attr('src', xhr_data).one('load', function()
			{
				$('div#cover_art_art_div').css('background-image', 'url("'+xhr_data+'")');
			});
		}
	});
}

function setCoverArtSize()
{
	if(!$('div#cover_art_art_div').length) return;

	var container_width = $('div#cover_art_div').outerWidth();
	var cover_art_width = $('div#cover_art_art_div').data('width');
	var cover_art_height = $('div#cover_art_art_div').data('height');

	if(cover_art_width > container_width)
	{
		var ratio = container_width / cover_art_width;
		var cover_art_height = Math.floor(cover_art_height * ratio);
		var minimum_height = $('div#cover_art_art_div').height();

		var size = (cover_art_height < minimum_height) ? 'auto '+minimum_height+'px' : container_width+'px auto';

		$('div#cover_art_art_div').css('background-size', size);
	}
	else
	{
		$('div#cover_art_art_div').css('background-size', cover_art_width+'px auto');
	}
}

// Recently played

function refreshRecentlyPlayedActivity()
{
	if(isActivity('recently-played', '')) refreshActivity();
}

function clearRecentlyPlayed()
{
	$.get('recently-played.php?clear', function()
	{
		refreshRecentlyPlayedActivity();
	});
}

// Queue

function queueUri(artist, title, uri)
{
	$.post('queue.php?queue_uri&'+getCurrentTime(), { artist: artist, title: title, uri: uri }, function(xhr_data)
	{
		if(xhr_data == 'spotify_is_not_running')
		{
			showToast('Spotify is not running', 2);
		}
		else
		{
			showToast('Track queued', 2);
			refreshQueueActivity();
		}
	});
}

function queueUris(uris, randomly)
{
	$.post('queue.php?queue_uris&'+getCurrentTime(), { uris: uris, randomly: randomly }, function(xhr_data)
	{
		if(xhr_data == 'spotify_is_not_running')
		{
			showToast('Spotify is not running', 2);
		}
		else if(xhr_data == 'error')
		{
			showToast('Could not queue tracks', 2);
		}
		else
		{
			var number = parseInt(xhr_data);
			var toast = (number == 1) ? 'track' : 'tracks';

			showToast(number+' '+toast+' queued', 2);
			refreshQueueActivity();
		}
	});
}

function moveQueuedUri(id, sortorder, direction)
{
	$.post('queue.php?move&'+getCurrentTime(), { id: id, sortorder: sortorder, direction: direction }, function(xhr_data)
	{
		refreshQueueActivity();
	});
}

function removeFromQueue(id, sortorder)
{
	$.post('queue.php?remove&'+getCurrentTime(), { id: id, sortorder: sortorder }, function()
	{
		refreshQueueActivity();
	});
}

function queueAction(queue_action, sortorder)
{
	$.post('queue.php?action&'+getCurrentTime(), { queue_action: queue_action, sortorder: sortorder }, function()
	{
		if(sortorder == 'top')
		{
			changeActivity('queue', '', '');
		}
		else
		{
			refreshQueueActivity();
		}

		showToast('Action added to queue', 2);
	});
}

function clearQueue()
{
	$.get('queue.php?clear', function()
	{
		refreshQueueActivity();
	});
}

function refreshQueueActivity()
{
	if(isActivity('queue', '')) refreshActivity();
}

// Playlists

function browsePlaylist(uri, description, is_authorized_with_spotify)
{
	if(is_authorized_with_spotify)
	{
		if(description != '')
		{
			var cookie_value = {};
			cookie_value[uri] = description;
			cookie_value = JSON.stringify(cookie_value);
		}

		$.cookie('playlist_description', cookie_value, { expires: 3650 });

		changeActivity('playlists', 'browse', 'uri='+uri);
	}
	else
	{
		changeActivity('profile', '', '');
	}
}

function addToPlaylist(title, uri, is_authorized_with_spotify)
{
	if(is_authorized_with_spotify)
	{
		if(getUriType(uri) == 'local')
		{
			showToast('Not possible for local files', 4);
		}
		else
		{
			$.get('playlists.php?get_playlists_with_starred', function(xhr_data)
			{
				var playlists = $.parseJSON(xhr_data);

				var actions = [];

				var i = 0;

				for(var playlist in playlists)
				{
					actions[i] = { text: hsc(playlist), keys: ['actions', 'uri', 'uris'], values: ['hide_dialog add_uris_to_playlist', playlists[playlist], uri] }

					i++;
				}

				showActionsDialog({ title: hsc(title), actions: actions });
			});
		}
	}
	else
	{
		changeActivity('profile', '', '');
	}
}

function addUrisToPlaylist(uri, uris)
{
	$.post('playlists.php?add_uris_to_playlist&'+getCurrentTime(), { uri: uri, uris: uris }, function(xhr_data)
	{
		if(xhr_data == 'no_access')
		{
			showToast('No access to playlist', 4);
		}
		else if(xhr_data == 'error')
		{
			showToast('Could not add to playlist', 4);
		}
		else
		{
			var toast = (parseInt(xhr_data) == 1) ? 'track' : 'tracks';
			showToast(xhr_data+' '+toast+' added to playlist', 2);

			refreshBrowsePlaylistActivity(uri);
		}
	});
}

function deleteUrisFromPlaylist(uri, uris, positions, snapshot_id, div)
{
	if(getUriType(uris) == 'local')
	{
		showToast('Not possible for local files', 4);
	}
	else
	{
		$.post('playlists.php?delete_uris_from_playlist&'+getCurrentTime(), { uri: uri, uris: uris, positions: positions, snapshot_id: snapshot_id }, function(xhr_data)
		{
			if(xhr_data == 'no_access')
			{
				showToast('No access to playlist', 4);
			}
			else if(xhr_data == 'error')
			{
				showToast('Could not delete from playlist', 4);
			}
			else
			{
				$('div#'+div).remove();

				showToast('Track deleted from playlist', 2);

				var span = $('span#playlist_tracks_count_span');
				var count = parseInt(span.html()) - 1;

				span.html(count);
			}
		});
	}
}

function confirmImportSpotifyPlaylists(is_authorized_with_spotify)
{
	if(is_authorized_with_spotify)
	{
		showDialog({ title: 'Import from Spotify', body_class: 'dialog_message_div', body_text: 'This will import your playlists from Spotify. Collaborative playlists will not be imported because of a limitation in Spotify\'s web API. You can temporarily mark them as not collaborative or import them manually.<br><br>Continue?', button1: { text: 'No', keys : ['actions'], values: ['hide_dialog'] }, button2: { text: 'Yes', keys : ['actions'], values: ['hide_dialog import_spotify_playlists'] } });
	}
	else
	{
		changeActivity('profile', '', '');
	}
}

function importSpotifyPlaylists(refresh)
{
	if(!refresh) activityLoading();

	xhr_activity = $.get('playlists.php?import_spotify_playlists', function(xhr_data)
	{
		var number = parseInt(xhr_data);
		var toast = (number == 1) ? 'playlist' : 'playlists';

		if(refresh)
		{
			if(xhr_data == 'no_playlists')
			{
				showToast('No playlists found', 4);
			}
			else if(xhr_data == 'error')
			{
				showToast('Could not refresh playlists', 4);
			}
			else if(number != 0)
			{
				showToast(number+' '+toast+' added', 2);

				refreshPlaylistsActivity();
			}
		}
		else
		{
			changeActivity('playlists', '', '');

			if(xhr_data == 'no_playlists')
			{
				showToast('No playlists found', 4);
			}
			else if(xhr_data == 'error')
			{
				showToast('Could not import playlists', 4);
			}
			else
			{
				showToast(number+' '+toast+' imported', 2);
			}
		}
	});

	$.cookie('last_playlists_import', getCurrentTime(), { expires: 3650 });
}

function importPlaylists(uris)
{
	var uris = $.trim(uris);
	var validate = uris.split(' ');

	var invalid = false;

	for(var i = 0; i < validate.length; i++)
	{
		var uri = urlToUri(validate[i]);

		if(getUriType(uri) == 'playlist')
		{
			cachePlaylist(uri);
		}
		else
		{
			invalid = true;
		}
	}

	if(invalid)
	{
		showToast('One or more invalid playlist URIs', 4);
		focusTextInput('input:text#import_playlists_uris_input');
	}
	else
	{
		blurTextInput();
		activityLoading();

		$.post('playlists.php?import_playlists&'+getCurrentTime(), { uris: uris }, function(xhr_data)
		{
			if(xhr_data == 'error')
			{
				showDialog({ title: 'Import Playlists', body_class: 'dialog_message_div', body_text: 'Could not import one or more playlists. Try again.', button1: { text: 'Close', keys : ['actions'], values: ['hide_dialog'] } });
			}
			else
			{
				var toast = (parseInt(xhr_data) == 1) ? 'playlist' : 'playlists';
				showToast(xhr_data+' '+toast+' imported', 2);
			}

			changeActivity('playlists', '', '');
		});
	}
}

function createPlaylist(name, make_public)
{
	if(name == '')
	{
		showToast('Playlist name can not be empty', 4);
		focusTextInput('input:text#create_playlist_name_input');
	}
	else
	{
		activityLoading();

		$.post('playlists.php?create_playlist&'+getCurrentTime(), { name: name, make_public: make_public }, function(xhr_data)
		{
			changeActivity('playlists', '', '');

			if(xhr_data == 'error')
			{
				showToast('Could not create playlist', 4);
			}
			else
			{
				showToast('Playlist "'+xhr_data+'" created', 2);
			}
		});
	}
}

function showEditPlaylistActivity(uri, is_authorized_with_spotify)
{
	if(is_authorized_with_spotify)
	{
		changeActivity('playlists', 'edit', 'uri='+uri);
	}
	else
	{
		changeActivity('profile', '', '');
	}
}

function editPlaylist(name, uri, make_public)
{
	if(name == '')
	{
		showToast('Playlist name can not be empty', 4);
		focusTextInput('input:text#edit_playlist_name_input');
	}
	else
	{
		activityLoading();

		$.post('playlists.php?edit_playlist&'+getCurrentTime(), { name: name, uri: uri, make_public: make_public }, function(xhr_data)
		{
			if(xhr_data == 'no_access')
			{
				changeActivity('playlists', '', '');

				showToast('No access to playlist', 4);
			}
			else if(xhr_data == 'error')
			{
				changeActivity('playlists', '', '');

				showToast('Could not edit playlist', 4);
			}
			else
			{
				changeActivity('playlists', '', '');

				showToast('Playlist "'+xhr_data+'" edited', 2);

				cachePlaylist(uri);
			}
		});
	}
}

function cachePlaylist(uri)
{
	$.post('playlists.php?cache_playlist&'+getCurrentTime(), { uri: uri });
}

function removePlaylist(id)
{
	$.post('playlists.php?remove_playlist&'+getCurrentTime(), { id: id }, function()
	{
		refreshPlaylistsActivity();
	});
}

function saveRecentPlaylist(uri)
{
	$.post('playlists.php?save_recent_playlists&'+getCurrentTime(), { uri: uri }, function()
	{
		refreshRecentPlaylistsActivity();

		cachePlaylist(uri);
	});
}

function clearRecentPlaylists()
{
	$.get('playlists.php?clear_recent_playlists', function()
	{
		refreshRecentPlaylistsActivity();
	});
}

function refreshPlaylist(uri)
{
	$.post('playlists.php?refresh_playlist&'+getCurrentTime(), { uri: uri }, function()
	{
		reloadActivity();
	});
}

function refreshPlaylistsActivity()
{
	if(isActivity('playlists', '')) refreshActivity();
}

function refreshBrowsePlaylistActivity(uri)
{
	var a  = getActivity();

	if(isActivity('playlists', 'browse') && shc(a.args, uri))
	{
		refreshActivity();
	}
	else
	{
		cachePlaylist(uri);
	}
}

function refreshRecentPlaylistsActivity()
{
	if(isActivity('playlists', 'recent-playlists')) refreshActivity();
}

// Library

function save(artist, title, uri, is_authorized_with_spotify, element)
{
	if(getUriType(uri) == 'local')
	{
		showToast('Not possible for local files', 4);
	}
	else if(is_authorized_with_spotify)
	{
		$.post('library.php?save&'+getCurrentTime(), { artist: artist, title: title, uri: uri }, function(xhr_data)
		{
			if(xhr_data == 'error')
			{
				showToast('Could not save track to library', 4);
			}
			else if(shc(xhr_data, 'removed'))
			{
				if($('div.img_div', element).length)
				{
					$('div.img_div', element).removeClass('remove_24_img_div').addClass('save_24_img_div');
				}
				else
				{
					var html = $(element).html();
					element.html(html.replace('Remove from Library', 'Save to Library'));
					showToast(xhr_data, 2);
				}
			}
			else
			{
				if($('div.img_div', element).length)
				{
					$('div.img_div', element).removeClass('save_24_img_div').addClass('remove_24_img_div');
				}
				else
				{
					var html = $(element).html();
					element.html(html.replace('Save to Library', 'Remove from Library'));
					showToast(xhr_data, 2);
				}
			}

			refreshLibraryActivity();
		});
	}
	else
	{
		changeActivity('profile', '', '');
	}
}

function remove(uri, is_authorized_with_spotify)
{
	if(is_authorized_with_spotify)
	{
		$.post('library.php?remove&'+getCurrentTime(), { uri: uri }, function(xhr_data)
		{
			refreshLibraryActivity();

			if(xhr_data == 'error') showToast('Could not remove track from library', 4);
		});
	}
	else
	{
		changeActivity('profile', '', '');
	}
}

function confirmImportSavedSpotifyTracks(is_authorized_with_spotify)
{
	if(is_authorized_with_spotify)
	{
		showDialog({ title: 'Import from Spotify', body_class: 'dialog_message_div', body_text: 'Your saved tracks are refreshed every five minutes. This action does it manually. Importing saved albums and artists is not currently supported.<br><br>Continue?', button1: { text: 'No', keys : ['actions'], values: ['hide_dialog'] }, button2: { text: 'Yes', keys : ['actions'], values: ['hide_dialog import_saved_spotify_tracks'] } });
	}
	else
	{
		changeActivity('profile', '', '');
	}
}

function importSavedSpotifyTracks(refresh)
{
	xhr_activity = $.get('library.php?import_saved_spotify_tracks', function(xhr_data)
	{
		var number = parseInt(xhr_data);
		var word = (Math.abs(number) == 1) ? 'track' : 'tracks';
		var toast = (number < 0) ? Math.abs(number)+' '+word+' removed' : number+' '+word+' imported';

		if(refresh)
		{
			if(xhr_data == 'no_tracks')
			{
				showToast('No saved tracks found', 4);
			}
			else if(xhr_data == 'error')
			{
				showToast('Could not refresh saved tracks', 4);
			}
			else if(number != 0)
			{
				showToast(toast, 2);

				refreshLibraryActivity();
			}
		}
		else
		{
			if(xhr_data == 'no_tracks')
			{
				showToast('No saved tracks found', 4);
			}
			else if(xhr_data == 'error')
			{
				showToast('Could not import saved tracks', 4);
			}
			else
			{
				refreshLibraryActivity();

				showToast(toast, 2);
			}
		}
	});

	$.cookie('last_saved_tracks_import', getCurrentTime(), { expires: 3650 });
}

function refreshLibraryActivity()
{
	if(isActivity('library', '')) refreshActivity();
}

// Browse

function getFeaturedPlaylists(fade_in_div)
{
	var time = parseInt(getCurrentTime() / 1000);
	var country = $('div#browse_featured_playlists_div').data('country');

	$.getJSON(project_website+'api/1/browse/featured-playlists/?time='+time+'&country='+country+'&fields='+encodeURIComponent('description,cover_art')+'&callback=?', function(json_data)
	{
		if(typeof json_data.metadata == 'undefined' || json_data.metadata == '')
		{
			showToast('Could not get featured playlists', 4);
		}
		else
		{
			var metadata = json_data.metadata;

			$('div#browse_featured_playlists_div > div').css('background-image', 'url("'+metadata.cover_art+'")');
			$('div#browse_featured_playlists_div > div > div').html(metadata.description);

			if(fade_in_div) fadeInDiv('div#browse_featured_playlists_div > div');
		}
	});
}

// Search

function getSearch(string)
{
	if(string == '')
	{
		showToast('Search can not be empty', 2);
		
		focusTextInput('input:text#search_input');
	}
	else
	{
		blurTextInput();

		$.cookie('settings_sort_search_tracks', 'default', { expires: 3650 });

		changeActivity('search', 'search', 'string='+string);
	}
}

function clearRecentSearches()
{
	$.get('search.php?clear', function()
	{
		refreshSearchActivity();
	});
}

function refreshSearchActivity()
{
	if(isActivity('search', '')) refreshActivity();
}

// Artists

function browseArtist(uri)
{
	if(getUriType(uri) == 'local')
	{
		showToast('Not possible for local files', 4);
	}
	else
	{
		changeActivity('artist', '', 'uri='+uri);
	}
}

function getArtistBiography(artist)
{
	var artist = decodeURIComponent(artist);

	var button = $('div#green_artist_biography_button_div');
	var button_text = button.html();

	button.html('Wait...');

	$.getJSON(project_website+'api/1/artist/biography/?artist='+encodeURIComponent(artist)+'&callback=?', function(json_data)
	{
		var metadata = json_data;

		if(metadata == '')
		{
			showToast('Could not get biography', 4);
		}
		else if(metadata.biography == '')
		{
			showToast('No biography available', 4);
		}
		else
		{
			button.hide();

			var from = (metadata.from == '') ? 'Unknown' : metadata.from;
			var formed = (metadata.formed == '') ? 'Unknown' : metadata.formed;
			var members = metadata.members;
			var listeners = (metadata.listeners_formatted == '') ? 'Unknown' : metadata.listeners_formatted;
			var plays = (metadata.plays_formatted == '') ? 'Unknown' : metadata.plays_formatted;
			var biography = (metadata.biography == '') ? 'Unknown' : metadata.biography;

			var html = '<div><b>From:</b> '+from+'</div><div><b>Formed:</b> '+formed+'</div>';

			if(metadata.members != '') html += '<div><b>Members:</b> '+members+'</div>';

			html += '<div><b>Listeners:</b> '+listeners+'</div><div><b>Plays:</b> '+plays+'</div><div>'+biography+'</div>';

			$('div#artist_biography_div').html(html).show();
		}

		button.html(button_text);
	});
}

// Albums

function browseAlbum(uri)
{
	if(uri == '') return;

	if(getUriType(uri) == 'local')
	{
		showToast('Not possible for local files', 4);
	}
	else
	{
		changeActivity('album', '', 'uri='+uri);
	}
}

// Profile

function authorizeWithSpotify()
{
	if(ua_is_ios && ua_is_standalone) $.removeCookie('current_activity_'+project_version);

	var uri = window.location.href.replace(window.location.hash, '')+'#profile//spotify_token=';
	var installed = parseInt($.cookie('installed_'+project_version));

	window.location.href = project_website+'api/1/spotify/authorize/?redirect_uri='+encodeURIComponent(uri)+'&state='+installed;
}

function confirmAuthorizeWithSpotify()
{
	showDialog({ title: 'Authorize with Spotify', body_class: 'dialog_message_div', body_text: 'This will redirect you to Spotify\'s website where you must log in as the same user you are logged in as in the Spotify desktop client.<br><br>Continue?', button1: { text: 'No', keys : ['actions'], values: ['hide_dialog'] }, button2: { text: 'Yes', keys : ['actions'], values: ['hide_dialog authorize_with_spotify'] } });
}

function deauthorizeFromSpotify()
{
	$.get('profile.php?deauthorize_from_spotify', function()
	{
		replaceActivity('profile', '', '');
	});
}

function confirmDeauthorizeFromSpotify()
{
	showDialog({ title: 'Deauthorize from Spotify', body_class: 'dialog_message_div', body_text: 'This will deauthorize you from Spotify. Normally you only do this if you are going to authorize as another Spotify user.<br><br>Continue?', button1: { text: 'No', keys : ['actions'], values: ['hide_dialog'] }, button2: { text: 'Yes', keys : ['actions'], values: ['hide_dialog deauthorize_from_spotify'] } });
}

// Settings

function saveSetting(setting, value)
{
	var cookie = { id: setting, value: value, expires: 3650 };
	$.cookie(cookie.id, cookie.value, { expires: cookie.expires });
	showToast('Tap top right icon to apply', 4);
}

function applySettings()
{
	if(ua_is_ios && ua_is_standalone) $.removeCookie('current_activity_'+project_version);
	window.location.replace('.');
}

function disableSetting(div)
{
	$(div+' > div.setting_text_div > div:first-child').addClass('setting_disabled_div');
	$(div+' > div.setting_text_div > div:last-child').html('Not supported on this device.');
	$(div+' > div.setting_edit_div > input.setting_checkbox').attr('disabled', 'disabled');
	$(div+' > div.setting_edit_div > select.setting_select').attr('disabled', 'disabled');
}

function confirmRemoveAllPlaylists()
{
		showDialog({ title: 'Remove All Playlists', body_class: 'dialog_message_div', body_text: 'This will remove all playlists from '+project_name+'. They will not be deleted from Spotify.<br><br>Continue?', button1: { text: 'No', keys : ['actions'], values: ['hide_dialog'] }, button2: { text: 'Yes', keys : ['actions'], values: ['hide_dialog remove_all_playlists'] } });
}

function removeAllPlaylists()
{
	$.get('playlists.php?remove_all_playlists', function()
	{
		showToast('All playlists removed', 2);

		$.removeCookie('last_playlists_import');
	});
}

function confirmClearCache()
{
	showDialog({ title: 'Clear Cache', body_class: 'dialog_message_div', body_text: 'This will clear the cache for playlists, albums, etc.<br><br>Continue?', button1: { text: 'No', keys : ['actions'], values: ['hide_dialog'] }, button2: { text: 'Yes', keys : ['actions'], values: ['hide_dialog clear_cache'] } });
}

function clearCache()
{
	$.post('main.php?'+getCurrentTime(), { action: 'clear_cache' }, function()
	{
		showToast('Cache cleared', 2);
	});
}

function confirmRestoreToDefault()
{
	showDialog({ title: 'Restore', body_class: 'dialog_message_div', body_text: 'This will restore settings, messages, etc.<br><br>Continue?', button1: { text: 'No', keys : ['actions'], values: ['hide_dialog'] }, button2: { text: 'Yes', keys : ['actions'], values: ['hide_dialog restore_to_default'] } });
}

function restoreToDefault()
{
	activityLoading();

	setTimeout(function()
	{
		removeAllCookies();
		window.location.replace('.');
	}, 2000);
}

// Share

function shareUri(title, uri)
{
	if(getUriType(decodeURIComponent(uri)) == 'local')
	{
		showToast('Not possible for local files', 4);
	}
	else if(ua_is_android_app)
	{
		setTimeout(function()
		{
			Android.JSshare(title, decodeURIComponent(uri));
		}, 250);
	}
	else
	{
		showDialog({ title: title, body_class: 'dialog_share_div', body_text: '<div title="Share on Facebook" class="actions_div" data-actions="open_external_activity" data-uri="https://www.facebook.com/sharer/sharer.php?u='+uri+'" data-highlightclass="light_grey_highlight" onclick="void(0)"><div class="img_div img_48_div facebook_48_img_div"></div></div><div title="Share on Twitter" class="actions_div" data-actions="open_external_activity" data-uri="https://twitter.com/intent/tweet?url='+uri+'" data-highlightclass="light_grey_highlight" onclick="void(0)"><div class="img_div img_48_div twitter_48_img_div"></div></div><div title="Share on Google+" class="actions_div" data-actions="open_external_activity" data-uri="https://plus.google.com/share?url='+uri+'" data-highlightclass="light_grey_highlight" onclick="void(0)"><div class="img_div img_48_div googleplus_48_img_div"></div></div>', button1: { text: 'Close', keys : ['actions'], values: ['hide_dialog'] } });
	}
}

// UI

function setCss()
{
	$('style', 'head').empty();

	window_width = $(window).width();
	window_height = $(window).height();

	if(ua_is_android && !ua_is_standalone || ua_is_ios && !ua_is_standalone)
	{
		var padding = parseInt($('div#activity_div').css('padding-top'));
		var min_height = window_height - padding * 2 + 128;

		$('div#activity_div').css('min-height', min_height+'px');
	}

	$('div#nowplaying_div').css('bottom', '-'+window_height+'px');

	$('div#menu_div').data('cssleft', $('div#menu_div').css('left'));
	$('div#nowplaying_div').data('cssbottom', $('div#nowplaying_div').css('bottom'));

	$('style', 'head').append('.show_nowplaying_animation { transform: translate3d(0, -'+window_height+'px, 0); -webkit-transform: translate3d(0, -'+window_height+'px, 0); -moz-transform: translate3d(0, -'+window_height+'px, 0); -ms-transform: translate3d(0, -'+window_height+'px, 0); } .hide_nowplaying_animation { transform: translate3d(0, 0, 0); -webkit-transform: translate3d(0, 0, 0); -moz-transform: translate3d(0, 0, 0); -ms-transform: translate3d(0, 0, 0); }');
}

function showDiv(div)
{
	$(div).css('opacity', '1');
}

function hideDiv(div)
{
	$(div).removeClass('show_div_animation hide_div_animation').css('opacity', '0');
}

function fadeInDiv(div)
{
	setTimeout(function()
	{
		if(ua_supports_csstransitions)
		{
			$(div).removeClass('hide_div_animation').addClass('show_div_animation');
		}
		else
		{
			$(div).stop().animate({ opacity: '1' }, 250, 'easeInCubic');
		}
	}, 25);
}

function fadeOutDiv(div)
{
	setTimeout(function()
	{
		if(ua_supports_csstransitions)
		{
			$(div).removeClass('show_div_animation').addClass('hide_div_animation');
		}
		else
		{
			$(div).stop().animate({ opacity: '0' }, 250, 'easeInCubic');
		}
	}, 25);
}

function setCardVerticalSize()
{
	if(!$('div.cards_vertical_div').length) return;

	var container_width = $('div.cards_vertical_div').outerWidth();
	var margin = parseInt($('div.card_vertical_div').css('margin-right'));
	var divide = 6;

	if(container_width <= 480)
	{
		divide = 2;
	}
	else if(container_width <= 640)
	{
		divide = 3;
	}
	else if(container_width <= 800)
	{
		divide = 4;
	}
	else if(container_width <= 960)
	{
		divide = 5;
	}

	var size = parseInt(container_width / divide) - margin * 2;

	$('div.card_vertical_div').width(size);
	$('div.card_vertical_cover_art_div').height(size);

	$('div.cards_vertical_div').css('visibility', 'visible');
}

function setEllipsis()
{
	$('div.card_vertical_upper_tall_div').dotdotdot();
}

function focusTextInput(id)
{
	if(ua_supports_touch)
	{
		$('input:text').blur();
	}
	else
	{
		$(id).focus();
	}
}

function blurTextInput()
{
	$('input:text').blur();
}

function scrollToTop(animate)
{
	if(getScrollPosition() != 0)
	{
		if(animate)
		{
			$('html, body').animate({ scrollTop: 0 }, 250);
		}
		else
		{
			setScrollPosition(0);
		}
	}
}

function showListItemActions(element)
{
	$('div#cover_art_actions_div > div').removeClass('green_opacity_highlight');
	
	var list_item_div = element.parent();
	var list_item_main_div = element;
	var list_item_main_actions_arrow_div = $('div.list_item_main_actions_arrow_div', list_item_main_div);
	var list_item_main_corner_arrow_div = $('div.list_item_main_corner_arrow_div', list_item_main_div);
	var list_item_actions_div = $('div.list_item_actions_div', list_item_div);

	list_item_div.css('border-bottom-width', '0');

	list_item_main_corner_arrow_div.hide();
	list_item_main_actions_arrow_div.show();

	hideDiv(list_item_actions_div);
	list_item_actions_div.show();
	fadeInDiv(list_item_actions_div);
}

function hideListItemActions()
{
	$('div.list_item_div').css('border-bottom-width', '').removeClass('lighter_grey_highlight');
	$('div.list_item_main_corner_arrow_div').show();
	$('div.list_item_main_actions_arrow_div').hide().removeClass('up_arrow_dark_grey_highlight');
	$('div.list_item_actions_div').hide();
	$('div.list_item_actions_inner_div > div').removeClass('dark_grey_highlight');
}

// Toasts

function showToast(text, duration)
{
	clearTimeout(timeout_show_toast);
	clearTimeout(timeout_hide_toast_1);
	clearTimeout(timeout_hide_toast_2);

	$('div#toast_div > div > div').html(text);
	$('div#toast_div').show();

	var width = $('div#toast_div').outerWidth();
	var height = $('div#toast_div').outerHeight();
	var margin = parseInt(width / 2);
	var border_radius = parseInt(height / 2);

	$('div#toast_div').css('margin-left', '-'+margin+'px').css('border-radius', border_radius+'px');

	fadeInDiv('div#toast_div');

	var duration = parseInt(duration * 1000);

	timeout_show_toast = setTimeout(function()
	{
		clearTimeout(timeout_hide_toast_1);
		clearTimeout(timeout_hide_toast_2);

		timeout_hide_toast_1 = setTimeout(function()
		{
			fadeOutDiv('div#toast_div');

			timeout_hide_toast_2 = setTimeout(function()
			{
				$('div#toast_div').hide();
			}, 250);
		}, duration);
	}, 250);
}

// Dialogs

function showDialog(dialog)
{
	if(isDisplayed('div#dialog_div')) return;

	$('div#black_cover_div').show();

	setTimeout(function()
	{
		if(ua_supports_csstransitions)
		{
			$('div#black_cover_div').removeClass('show_black_cover_div_animation hide_black_cover_div_animation').addClass('show_black_cover_div_animation');
		}
		else
		{
			$('div#black_cover_div').stop().animate({ opacity: '0.5' }, 250, 'easeOutQuad');
		}
	}, 25);

	setTimeout(function()
	{
		$('div#dialog_div').html('<div title="'+dialog.title+'" id="dialog_header_div">'+dialog.title+'</div><div id="dialog_body_div"><div id="'+dialog.body_class+'">'+dialog.body_text+'</div></div><div id="dialog_buttons_div"><div id="dialog_button1_div" class="actions_div" data-highlightclass="light_grey_highlight" onclick="void(0)">'+dialog.button1.text+'</div></div>');

		for(var i = 0; i < dialog.button1.keys.length; i++)
		{
			$('div#dialog_button1_div').data(dialog.button1.keys[i], dialog.button1.values[i]);
		}

		if(typeof dialog.button2 != 'undefined')
		{
			$('div#dialog_buttons_div').append('<div id="dialog_button2_div" class="actions_div" data-highlightclass="light_grey_highlight" onclick="void(0)">'+dialog.button2.text+'</div>');

			for(var i = 0; i < dialog.button2.keys.length; i++)
			{
				$('div#dialog_button2_div').data(dialog.button2.keys[i], dialog.button2.values[i]);
			}
		}

		$('div#dialog_div').show();

		var height = $('div#dialog_div').outerHeight();
		var margin = parseInt(height / 2);

		$('div#dialog_div').css('margin-top', '-'+margin+'px');

		var max_body_height = parseInt($('div#dialog_body_div').css('max-height'));
		var body_height = $('div#dialog_body_div')[0].scrollHeight;

		if(body_height > max_body_height)
		{
			scrolling_black_cover_div = true;

			$('div#dialog_body_div').css('overflow-y', 'scroll');
		}

		if(ua_supports_csstransitions && ua_supports_csstransforms3d)
		{
			$('div#dialog_div').addClass('prepare_dialog_animation');

			setTimeout(function()
			{
				$('div#dialog_div').addClass('show_dialog_animation');
			}, 25);
		}
		else
		{
			fadeInDiv('div#dialog_div');
		}

		nativeAppCanCloseCover();
	}, 125);
}

function showActionsDialog(dialog)
{
	var title = dialog.title;
	var actions = dialog.actions;
	var body = '';

	for(var i = 0; i < actions.length; i++)
	{
		var data = '';

		for(var f = 0; f < actions[i].keys.length; f++)
		{
			data += 'data-'+actions[i].keys[f]+'="'+actions[i].values[f]+'" ';
		}

		body += '<div title="'+actions[i].text+'" class="actions_div" '+data+' data-highlightclass="light_grey_highlight" onclick="void(0)">'+actions[i].text+'</div>';
	}

	showDialog({ title: title, body_class: 'dialog_actions_div', body_text: body, button1: { text: 'Close', keys : ['actions'], values: ['hide_dialog'] } });
}

function showDetailsDialog(dialog)
{
	var title = dialog.title;
	var details = dialog.details;
	var body = '';

	for(var i = 0; i < details.length; i++)
	{
		body += '<div title="'+details[i].value+'"><b>'+details[i].detail+':</b> '+details[i].value+'</div>';
	}

	showDialog({ title: title, body_class: 'dialog_details_div', body_text: body, button1: { text: 'Close', keys : ['actions'], values: ['hide_dialog'] } });
}

function hideDialog()
{
	if(!isDisplayed('div#dialog_div')) return;

	scrolling_black_cover_div = false;

	$('div#dialog_div').hide();

	if(ua_supports_csstransitions && ua_supports_csstransforms3d)
	{
		$('div#dialog_div').removeClass('prepare_dialog_animation show_dialog_animation');
	}
	else
	{
		hideDiv('div#dialog_div');
	}

	setTimeout(function()
	{
		if(isDisplayed('div#dialog_div')) return;

		if(ua_supports_csstransitions)
		{
			$('div#black_cover_div').addClass('hide_black_cover_div_animation').one(event_transitionend, function()
			{
				if(!isDisplayed('div#dialog_div')) $('div#black_cover_div').hide().removeClass('show_black_cover_div_animation hide_black_cover_div_animation');
			});
		}
		else
		{
			$('div#black_cover_div').stop().animate({ opacity: '0' }, 250, 'easeOutQuad', function()
			{
				if(!isDisplayed('div#dialog_div')) $('div#black_cover_div').hide();
			});
		}
	}, 25);

	setTimeout(function()
	{
		checkForDialogs();
	}, 500);

	nativeAppCanCloseCover();
}

function closeDialog()
{
	if(!isDisplayed('div#dialog_div')) return;

	pointer_moved = false;
	$('div#dialog_button1_div').trigger(pointer_event);
}

function checkForDialogs()
{
	if(isDisplayed('div#dialog_div')) return;

	if(!ua_is_supported)
	{
		var cookie = { id: 'hide_unsupported_browser_dialog_'+project_version, value: 'true', expires: 7 };
		if(!isCookie(cookie.id)) showDialog({ title: 'Browser Warning', body_class: 'dialog_message_div', body_text: 'You are using an unsupported browser. If things do not work as they should, you know why.', button1: { text: 'Close', keys : ['actions', 'cookieid', 'cookievalue', 'cookieexpires'], values: ['hide_dialog set_cookie', cookie.id, cookie.value, cookie.expires] }, button2: { text: 'Help', keys : ['actions', 'uri'], values: ['open_external_activity', project_website+'?requirements'] } });
	}

	if(!ua_supports_csstransitions || !ua_supports_csstransforms3d)
	{
		var cookie = { id: 'hide_software_accelerated_animations_dialog_'+project_version, value: 'true', expires: 3650 };
		if(!isCookie(cookie.id)) showDialog({ title: 'Browser Warning', body_class: 'dialog_message_div', body_text: 'Your browser does not fully support hardware accelerated animations. Simple animations will be used instead, which may result in a less elegant experience.', button1: { text: 'Close', keys : ['actions', 'cookieid', 'cookievalue', 'cookieexpires'], values: ['hide_dialog set_cookie', cookie.id, cookie.value, cookie.expires] }, button2: { text: 'Help', keys : ['actions', 'uri'], values: ['open_external_activity', project_website+'?requirements'] } });
	}

	var latest_version = $.cookie('latest_version');

	if(settings_check_for_updates && parseFloat(latest_version) > project_version)
	{
		var cookie = { id: 'hide_update_available_dialog_'+project_version, value: 'true', expires: 7 };
		if(!isCookie(cookie.id)) showDialog({ title: 'Update Available', body_class: 'dialog_message_div', body_text: project_name+' '+latest_version+' has been released!', button1: { text: 'Close', keys : ['actions', 'cookieid', 'cookievalue', 'cookieexpires'], values: ['hide_dialog set_cookie', cookie.id, cookie.value, cookie.expires] }, button2: { text: 'Download', keys : ['actions', 'uri'], values: ['open_external_activity', project_website+'?download'] } });
	}

	if(ua_is_android)
	{
		if(ua_is_android_app)
		{
			var cookie = { id: 'hide_android_app_versions_mismatch_dialog_'+project_version, value: 'true', expires: 1 };

			if(!isCookie(cookie.id))
			{
				var versions = $.parseJSON(Android.JSgetVersions());

				var app_version = project_version;
				var app_minimum_version = parseFloat(versions[1]);

				var android_app_version = parseFloat(versions[0]);
				var android_app_minimum_version = project_android_app_minimum_version;

				if(app_version < app_minimum_version || android_app_version < android_app_minimum_version) showDialog({ title: 'App Versions Mismatch', body_class: 'dialog_message_div', body_text: 'The '+project_name+' version you are running is not compatible with this Android app version. Make sure you are running the latest version of both '+project_name+' and the Android app.', button1: { text: 'Close', keys : ['actions', 'cookieid', 'cookievalue', 'cookieexpires'], values: ['hide_dialog set_cookie', cookie.id, cookie.value, cookie.expires] } });
			}

			var cookie = { id: 'hide_android_hardware_buttons_dialog_'+project_version, value: 'true', expires: 3650 };
			if(!isCookie(cookie.id)) showDialog({ title: 'Android App Tip', body_class: 'dialog_message_div', body_text: 'You can use the hardware volume buttons on your device to control Spotify\'s volume. There are also some extra features that can be enabled in Settings.', button1: { text: 'Close', keys : ['actions', 'cookieid', 'cookievalue', 'cookieexpires'], values: ['hide_dialog set_cookie', cookie.id, cookie.value, cookie.expires] } });

			var installed = parseInt($.cookie('installed_'+project_version));
			var cookie = { id: 'hide_rate_on_google_play_dialog_'+project_version, value: 'true', expires: 3650 };

			if(!isCookie(cookie.id) && getCurrentTime() > installed + 1000 * 3600 * 24)
			{
				var package_name = Android.JSgetPackageName();
				showDialog({ title: 'Like this App?', body_class: 'dialog_message_div', body_text: 'Please rate '+project_name+' on Google Play.', button1: { text: 'Close', keys : ['actions', 'cookieid', 'cookievalue', 'cookieexpires'], values: ['hide_dialog set_cookie', cookie.id, cookie.value, cookie.expires] }, button2: { text: 'Rate', keys : ['actions', 'uri'], values: ['open_external_activity', 'market://details?id='+package_name] } });
			}
		}
		else
		{
			var cookie = { id: 'hide_android_app_dialog_'+project_version, value: 'true', expires: 1 };
			if(!isCookie(cookie.id)) showDialog({ title: 'Android App', body_class: 'dialog_message_div', body_text: 'You should install the Android app. It will give you an experience much more similar to a native app, with many additional features.', button1: { text: 'Close', keys : ['actions', 'cookieid', 'cookievalue', 'cookieexpires'], values: ['hide_dialog set_cookie', cookie.id, cookie.value, cookie.expires] }, button2: { text: 'Download', keys : ['actions', 'uri'], values: ['open_external_activity', 'market://search?q=pub:'+encodeURIComponent(project_developer)] } });
		}
	}
	else if(ua_is_ios)
	{
		if(ua_is_standalone)
		{
			var cookie = { id: 'hide_ios_back_gesture_dialog_'+project_version, value: 'true', expires: 3650 };
			if(!isCookie(cookie.id)) showDialog({ title: 'iOS Tip', body_class: 'dialog_message_div', body_text: 'Since you are running fullscreen and your device has no back button, you can swipe in from the right to go back.', button1: { text: 'Close', keys : ['actions', 'cookieid', 'cookievalue', 'cookieexpires'], values: ['hide_dialog set_cookie', cookie.id, cookie.value, cookie.expires] } });
		}
		else
		{
			var cookie = { id: 'hide_ios_home_screen_dialog_'+project_version, value: 'true' };

			if(!isCookie(cookie.id))
			{
				if(shc(ua, 'iPad'))
				{
					cookie.expires = 28;
					showDialog({ title: 'iPad Tip', body_class: 'dialog_message_div', body_text: 'Add '+project_name+' to your home screen to get fullscreen like a native app.', button1: { text: 'Close', keys : ['actions', 'cookieid', 'cookievalue', 'cookieexpires'], values: ['hide_dialog set_cookie', cookie.id, cookie.value, cookie.expires] }, button2: { text: 'How to', keys : ['actions', 'uri'], values: ['open_external_activity', project_website+'?add_to_home_screen'] } });
				}
				else
				{
					cookie.expires = 1;
					showDialog({ title: 'iPhone/iPod Warning', body_class: 'dialog_message_div', body_text: 'To function correctly, '+project_name+' should be added to your home screen.', button1: { text: 'Close', keys : ['actions', 'cookieid', 'cookievalue', 'cookieexpires'], values: ['hide_dialog set_cookie', cookie.id, cookie.value, cookie.expires] }, button2: { text: 'How to', keys : ['actions', 'uri'], values: ['open_external_activity', project_website+'?add_to_home_screen'] } });
				}
			}
		}
	}
	else if(ua_is_os_x && !ua_is_standalone)
	{
		var cookie = { id: 'hide_ox_x_integration_dialog_'+project_version, value: 'true', expires: 3650 };
		if(!isCookie(cookie.id)) showDialog({ title: 'OS X Tip', body_class: 'dialog_message_div', body_text: 'Install Fluid to run '+project_name+' as a standalone app.', button1: { text: 'Close', keys : ['actions', 'cookieid', 'cookievalue', 'cookieexpires'], values: ['hide_dialog set_cookie', cookie.id, cookie.value, cookie.expires] }, button2: { text: 'How to', keys : ['actions', 'uri'], values: ['open_external_activity', project_website+'?os_x_integration'] } });
	}
	else if(ua_is_msie && ua_is_pinnable_msie && !window.external.msIsSiteMode())
	{
		var cookie = { id: 'hide_windows_desktop_integration_dialog_'+project_version, value: 'true', expires: 3650 };
		if(!isCookie(cookie.id)) showDialog({ title: 'Windows Desktop Tip', body_class: 'dialog_message_div', body_text: 'Pin '+project_name+' to the taskbar to get additional features.', button1: { text: 'Close', keys : ['actions', 'cookieid', 'cookievalue', 'cookieexpires'], values: ['hide_dialog set_cookie', cookie.id, cookie.value, cookie.expires] }, button2: { text: 'How to', keys : ['actions', 'uri'], values: ['open_external_activity', project_website+'?windows_desktop_integration'] } });
	}
}

// Notifications

function requestNotificationsPermission()
{
	if(!ua_supports_notifications) return;

	if(Notification.permission != 'denied') Notification.requestPermission();
}

function showNotification(title, body, icon, onclick, duration)
{
	if(!settings_notifications || !ua_supports_notifications) return;

	if(notification != null) notification.close();

	notification = new Notification(title, { body: body, icon: icon });

	notification.onclick = function()
	{
		if(onclick == 'remote_control_next')
		{
				remoteControl('next');
		}
	}

	var duration = parseInt(duration * 1000);

	clearTimeout(timeout_notification);

	timeout_notification = setTimeout(function()
	{
		notification.close();
	}, duration);
}

// Scrolling

function getScrollPosition()
{
	return $(window).scrollTop();
}

function setScrollPosition(position)
{
	window.scrollTo(0, position);
}

function saveScrollPosition(action)
{
	if(scroll_position_save_disable) return;

	var a = getActivity();
	var time = a.time;

	var position = getScrollPosition();
	var action = (typeof scroll_position[time] != 'undefined' && scroll_position[time].action != null) ? scroll_position[time].action : action;

	scroll_position[time] = { position: position, action: action };

	if(ua_is_ios && ua_is_standalone)
	{
		var cookie = { id: 'current_activity_'+project_version, expires: 1 };

		if(isCookie(cookie.id))
		{
			var cookie_a = $.parseJSON($.cookie(cookie.id));
			cookie_a.scroll_position = position;

			$.cookie(cookie.id, JSON.stringify(cookie_a), { expires: cookie.expires });
		}
	}
}

function restoreScrollPosition()
{
	var a = getActivity();
	var time = a.time;

	if($.isEmptyObject(scroll_position) && ua_is_ios && ua_is_standalone)
	{
		var cookie = { id: 'current_activity_'+project_version };

		if(isCookie(cookie.id))
		{
			var cookie_a = $.parseJSON($.cookie(cookie.id));

			setScrollPosition(cookie_a.scroll_position);
		}

		return;	
	}
	else if(typeof scroll_position[time] == 'undefined')
	{
		return;
	}
	
	var position = scroll_position[time].position;
	var action = scroll_position[time].action;

	if(action != null)
	{
		if(action.action == 'show_all_list_items')
		{
			$(action.showitems).show();
			$(action.hideitem).hide();
		}
	}

	setScrollPosition(position);
}

// Native apps

function nativeAppLoad(is_paused)
{
	if(ua_is_android_app)
	{
		if(settings_keep_screen_on)
		{
			Android.JSkeepScreenOn(true);
		}
		else
		{
			Android.JSkeepScreenOn(false);
		}

		if(settings_pause_on_incoming_call)
		{
			Android.JSsetSharedBoolean('PAUSE_ON_INCOMING_CALL', true);
		}
		else
		{
			Android.JSsetSharedBoolean('PAUSE_ON_INCOMING_CALL', false);
		}

		if(settings_pause_on_outgoing_call)
		{
			Android.JSsetSharedBoolean('PAUSE_ON_OUTGOING_CALL', true);
		}
		else
		{
			Android.JSsetSharedBoolean('PAUSE_ON_OUTGOING_CALL', false);
		}

		if(settings_flip_to_pause)
		{
			Android.JSsetSharedBoolean('FLIP_TO_PAUSE', true);
		}
		else
		{
			Android.JSsetSharedBoolean('FLIP_TO_PAUSE', false);
		}

		if(settings_shake_to_skip)
		{
			Android.JSsetSharedBoolean('SHAKE_TO_SKIP', true);
			Android.JSsetSharedString('SHAKE_TO_SKIP_SENSITIVITY', settings_shake_sensitivity);
		}
		else
		{
			Android.JSsetSharedBoolean('SHAKE_TO_SKIP', false);
		}

		if(settings_persistent_notification)
		{
			Android.JSsetSharedBoolean('PERSISTENT_NOTIFICATION', true);
		}
		else
		{
			Android.JSsetSharedBoolean('PERSISTENT_NOTIFICATION', false);
		}

		var shareUri = urlToUri(Android.JSgetSharedString('SHARE_URI'));

		if(shareUri != '')
		{
			var type = getUriType(shareUri);

			if(type == 'playlist' || type == 'starred')
			{
				$.get('profile.php?is_authorized_with_spotify', function(xhr_data)
				{
					browsePlaylist(shareUri, '', stringToBoolean(xhr_data));
				});
			}
			else if(type == 'track' || type == 'album')
			{
				browseAlbum(shareUri);
			}
			else if(type == 'artist')
			{
				browseArtist(shareUri);
			}
			else
			{
				showToast('Invalid URI', 4);
			}

			Android.JSsetSharedString('SHARE_URI', '');
		}

		if('JSstartService' in window.Android) Android.JSstartService();
	}
}

function nativeAppAction(action)
{
	if(action == 'play_pause')
	{
		remoteControl('play_pause');
	}
	else if(action == 'pause')
	{
		remoteControl('pause');
	}
	else if(action == 'volume_down')
	{
		adjustVolume('down');
	}
	else if(action == 'volume_up')
	{
		adjustVolume('up');
	}
	else if(action == 'back')
	{
		goBack();
	}
	else if(action == 'menu')
	{
		toggleMenu();
	}
	else if(action == 'search')
	{
		changeActivity('search', '', '');
	}
}

function changeNativeAppComputer()
{
	if(ua_is_android_app) showDialog({ title: 'Change Computer', body_class: 'dialog_message_div', body_text: 'You can always go back to the list of computers by long-pressing the back button on your device.<br><br>Continue?', button1: { text: 'No', keys : ['actions'], values: ['hide_dialog'] }, button2: { text: 'Yes', keys: ['actions'], values: ['confirm_change_native_app_computer'] } });
}

function confirmChangeNativeAppComputer()
{
	if(ua_is_android_app) Android.JSfinishActivity();
}

function nativeAppCanCloseCover()
{
	if(menuIsVisible() || isDisplayed('div#top_actionbar_overflow_actions_div') || isVisible('div#nowplaying_div') || isDisplayed('div#nowplaying_actionbar_overflow_actions_div') || isDisplayed('div#dialog_div'))
	{
		if(ua_is_android_app) Android.JSsetSharedBoolean('CAN_CLOSE_COVER', true);
	}
	else
	{
		if(ua_is_android_app) Android.JSsetSharedBoolean('CAN_CLOSE_COVER', false);
	}
}

// Desktop integration

function integrateInMSIE()
{
	try
	{
		ie_thumbnail_button_previous = window.external.msSiteModeAddThumbBarButton('img/previous.ico?'+project_serial, 'Previous');
		ie_thumbnail_button_play_pause = window.external.msSiteModeAddThumbBarButton('img/play.ico?'+project_serial, 'Play');
		ie_thumbnail_button_next = window.external.msSiteModeAddThumbBarButton('img/next.ico?'+project_serial, 'Next');
		ie_thumbnail_button_volume_mute = window.external.msSiteModeAddThumbBarButton('img/volume-mute.ico?'+project_serial, 'Mute');
		ie_thumbnail_button_volume_down = window.external.msSiteModeAddThumbBarButton('img/volume-down.ico?'+project_serial, 'Volume down');
		ie_thumbnail_button_volume_up = window.external.msSiteModeAddThumbBarButton('img/volume-up.ico?'+project_serial, 'Volume up');

		ie_thumbnail_button_style_play = 0;
		ie_thumbnail_button_style_pause = window.external.msSiteModeAddButtonStyle(ie_thumbnail_button_play_pause, 'img/pause.ico?'+project_serial, 'Pause');

		window.external.msSiteModeShowThumbBar();

		document.addEventListener('msthumbnailclick', onClickMSIEthumbnailButton, false);

		integrated_in_msie = true;
	}
	catch(exception)
	{

	}
}

function onClickMSIEthumbnailButton(button)
{
	if(button.buttonID == ie_thumbnail_button_previous)
	{
		remoteControl('previous');
	}
	else if(button.buttonID == ie_thumbnail_button_play_pause)
	{
		remoteControl('play_pause');
	}
	else if(button.buttonID == ie_thumbnail_button_next)
	{
		remoteControl('next');
	}
	else if(button.buttonID == ie_thumbnail_button_volume_mute)
	{
		adjustVolume('mute');
	}
	else if(button.buttonID == ie_thumbnail_button_volume_down)
	{
		adjustVolume('down');
	}
	else if(button.buttonID == ie_thumbnail_button_volume_up)
	{
		adjustVolume('up');
	}
}

// Keyboard shortcuts

function enableKeyboardShortcuts()
{
	$.getScript('js/mousetrap.js?'+project_serial, function()
	{
		$.getScript('js/keyboard-shortcuts.js?'+project_serial, function()
		{
			Mousetrap.bind('1', function(event) { onKeyboardKeyPressed('1', event); }, 'keyup');
			Mousetrap.bind('2', function(event) { onKeyboardKeyPressed('2', event); }, 'keyup');
			Mousetrap.bind('3', function(event) { onKeyboardKeyPressed('3', event); }, 'keyup');
			Mousetrap.bind('q', function(event) { onKeyboardKeyPressed('q', event); }, 'keyup');
			Mousetrap.bind('w', function(event) { onKeyboardKeyPressed('w', event); }, 'keyup');
			Mousetrap.bind('e', function(event) { onKeyboardKeyPressed('e', event); }, 'keyup');
			Mousetrap.bind('r', function(event) { onKeyboardKeyPressed('r', event); }, 'keyup');
			Mousetrap.bind('a', function(event) { onKeyboardKeyPressed('a', event); }, 'keyup');
			Mousetrap.bind('s', function(event) { onKeyboardKeyPressed('s', event); }, 'keyup');
			Mousetrap.bind('d', function(event) { onKeyboardKeyPressed('d', event); }, 'keyup');
			Mousetrap.bind('z', function(event) { onKeyboardKeyPressed('z', event); }, 'keyup');
			Mousetrap.bind('x', function(event) { onKeyboardKeyPressed('x', event); }, 'keyup');
			Mousetrap.bind('c', function(event) { onKeyboardKeyPressed('c', event); }, 'keyup');
			Mousetrap.bind('tab', function(event) { onKeyboardKeyPressed('tab', event); }, 'keydown');
			Mousetrap.bind('enter', function(event) { onKeyboardKeyPressed('enter', event); }, 'keyup');
			Mousetrap.bind('esc', function(event) { onKeyboardKeyPressed('esc', event); }, 'keyup');

			Mousetrap.stopCallback = function(event, element, key)
			{
				if($(element).is('input:text'))
				{
					if(key == 'tab')
					{
						event.preventDefault();

						blurTextInput();
					}

					return true;
				}
			}
		});
	});
}

// Check stuff

function checkForErrors()
{
	var cookie = { id: 'test', value: 'true' };
	$.cookie(cookie.id, cookie.value);

	var error_code = (!isCookie(cookie.id)) ? 5 : project_error_code;

	$.removeCookie(cookie.id);

	return error_code;
}

function checkForUpdates(type)
{
	var latest_version_cookie = { id: 'latest_version', expires: 3650 };
	var latest_version = parseFloat($.cookie(latest_version_cookie.id));

	var last_update_check_cookie = { id: 'last_update_check', value: getCurrentTime(), expires: 3650 };
	var last_update_check = $.cookie(last_update_check_cookie.id);

	if(type == 'manual')
	{
		activityLoading();

		xhr_activity = $.get('main.php?check_for_updates', function(xhr_data)
		{
			if(xhr_data == 'error')
			{
				$.removeCookie(latest_version_cookie.id);
			}
			else
			{
				$.cookie(latest_version_cookie.id, xhr_data, { expires: latest_version_cookie.expires });
			}

			changeActivity('settings', '', '');
		});

		$.cookie(last_update_check_cookie.id, last_update_check_cookie.value, { expires: last_update_check_cookie.expires });
	}
	else if(type == 'auto' && settings_check_for_updates)
	{
		if(!isCookie(last_update_check_cookie.id) || !isNaN(last_update_check) && getCurrentTime() - last_update_check > 1000 * 3600 * 24)
		{
			$.get('main.php?check_for_updates', function(xhr_data)
			{
				if(xhr_data == 'error')
				{
					$.removeCookie(latest_version_cookie.id);
				}
				else
				{
					$.cookie(latest_version_cookie.id, xhr_data, { expires: latest_version_cookie.expires });
				}
			});

			$.cookie(last_update_check_cookie.id, last_update_check_cookie.value, { expires: last_update_check_cookie.expires });
		}

		if(latest_version > project_version) $('div#update_available_indicator_div').removeClass('settings_24_img_div').addClass('update_available_24_img_div');
	}
}

function checkIfNewVersionIsInstalled(version)
{
	if(version > project_version) showDialog({ title: 'New Version Installed', body_class: 'dialog_message_div', body_text: 'A new version of '+project_name+' has been installed!<br><br>You must now start the daemon on the computer running Spotify. Tap How to below to find out how. After it is done, tap Reload.', button1: { text: 'Reload', keys : ['actions'], values: ['reload_app'] }, button2: { text: 'How to', keys : ['actions', 'uri'], values: ['open_external_activity', project_website+'?start_daemon'] } });
}

function booleanToString(bool)
{
	return (bool) ? 'true' : 'false';
}

function stringToBoolean(string)
{
	return (string == 'true');
}

function shc(string, characters)
{
	var string = string.toLowerCase();
	var characters = characters.toLowerCase();

	return (string.indexOf(characters) != -1);
}

function isDisplayed(id)
{
	return ($(id).is(':visible'));
}

function isVisible(id)
{
	return ($(id).css('visibility') == 'visible');
}

function menuIsVisible()
{
	return (isDisplayed('div#top_actionbar_inner_left_div') && isVisible('div#menu_div'))
}

function isWidescreen()
{
	if($(window).width() < 1024) return;

	$('div#menu_div').removeClass('show_menu_animation hide_menu_animation').css('visibility', '').css('left', '');
	$('div#top_actionbar_inner_left_div > div > div > div').removeClass('show_menu_img_animation hide_menu_img_animation');
	$('div#black_cover_activity_div').hide().removeClass('show_black_cover_activity_div_animation hide_black_cover_activity_div_animation').css('opacity', '');
}

function textInputHasFocus()
{
	return ($('input:text').is(':focus'));
}

function getCurrentTime()
{
	return new Date().getTime();
}

function getMSIEVersion()
{
	var re = ua.match(/Mozilla\/\d+\.\d+ \(Windows NT \d+\.\d+;.*Trident\/\d+\.\d+;.*rv:(\d+)\.\d+\)/);
	return (re) ? parseInt(re[1]) : 0;
}

// Manipulate stuff

function hsc(string)
{
	return String(string).replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;').replace(/'/g, '&#039;');
}

function ucfirst(string)
{
	return string.charAt(0).toUpperCase() + string.slice(1);
}

// URIs

function browseUri(uri, is_authorized_with_spotify)
{
	var uri = urlToUri(uri);
	var type = getUriType(uri);

	if(type == 'playlist')
	{
		browsePlaylist(uri, '', is_authorized_with_spotify);
	}
	else if(type == 'artist')
	{
		browseArtist(uri);
	}
	else if(type == 'album')
	{
		browseAlbum(uri);
	}
	else if(type == 'track')
	{
		playUri(uri);
	}
	else if(type == 'genre')
	{
		var genre = uri.replace(/^spotify:app:genre:(.*?)$/, '$1');

		changeActivity('browse', 'genres', 'name='+ucfirst(genre)+'&genre='+genre);
	}
	else if(uri.match(/^https?:\/\//))
	{
		openExternalActivity(uri);
	}
	else
	{
		showToast('Invalid URI', 4);
	}
}

function getUriType(uri)
{
	var type = 'unknown';

	if(uri.match(/^spotify:user:[^:]+:playlist:\w{22}$/) || uri.match(/^https?:\/\/open\.spotify\.com\/user\/[^\/]+\/playlist\/\w{22}$/))
	{
		type = 'playlist';
	}
	else if(uri.match(/^spotify:user:[^:]+:starred$/) || uri.match(/^https?:\/\/open\.spotify\.com\/user\/[^\/]+\/starred$/))
	{
		type = 'starred';
	}
	else if(uri.match(/^spotify:artist:\w{22}$/) || uri.match(/^https?:\/\/open\.spotify\.com\/artist\/\w{22}$/))
	{
		type = 'artist';
	}
	else if(uri.match(/^spotify:album:\w{22}$/) || uri.match(/^https?:\/\/open\.spotify\.com\/album\/\w{22}$/))
	{
		type = 'album';
	}
	else if(uri.match(/^spotify:track:\w{22}$/) || uri.match(/^https?:\/\/open\.spotify\.com\/track\/\w{22}$/))
	{
		type = 'track';
	}
	else if(uri.match(/^spotify:local:[^:]+:[^:]*:[^:]+:\d*$/) || uri.match(/^https?:\/\/open\.spotify\.com\/local\/[^\/]+\/[^\/]*\/[^\/]+\/\d*$/))
	{
		type = 'local';
	}
	else if(uri.match(/^spotify:app:genre:\w+$/) || uri.match(/^https?:\/\/spotify:app:genre:\w+$/))
	{
		type = 'genre';
	}
	else if(uri.match(/^https?:\/\/o\.scdn\.co\/\w+\/\w+$/) || uri.match(/^https?:\/\/\w+\.cloudfront\.net\/\w+\/\w+$/))
	{
		type = 'cover_art';
	}

	return type;
}

function uriToUrl(uri)
{
	var type = getUriType(uri);

	if(type == 'playlist')
	{
		uri = uri.replace(/^spotify:user:(.*?):playlist:(.*?)$/, 'http://open.spotify.com/user/$1/playlist/$2');
	}
	else if(type == 'starred')
	{
		uri = uri.replace(/^spotify:user:(.*?):starred$/, 'http://open.spotify.com/user/$1/starred');
	}
	else if(type == 'artist')
	{
		uri = uri.replace(/^spotify:artist:(.*?)$/, 'http://open.spotify.com/artist/$1');
	}
	else if(type == 'album')
	{
		uri = uri.replace(/^spotify:album:(.*?)$/, 'http://open.spotify.com/album/$1');
	}
	else if(type == 'track')
	{
		uri = uri.replace(/^spotify:track:(.*?)$/, 'http://open.spotify.com/track/$1');
	}
	else if(type == 'local')
	{
		uri = uri.replace(/:/g, '/').replace(/^spotify\/local\/(.*?)$/, 'http://open.spotify.com/local/$1');
	}

	return uri;
}

function urlToUri(uri)
{
	var type = getUriType(uri);

	if(type == 'playlist')
	{
		uri = uri.replace(/^https?:\/\/open\.spotify\.com\/user\/(.*?)\/playlist\/(.*?)$/, 'spotify:user:$1:playlist:$2');
	}
	else if(type == 'stssarred')
	{
		uri = uri.replace(/^https?:\/\/open\.spotify\.com\/user\/(.*?)\/starred$/, 'spotify:user:$1:starred');
	}
	else if(type == 'artist')
	{
		uri = uri.replace(/^https?:\/\/open\.spotify\.com\/artist\/(.*?)$/, 'spotify:artist:$1');
	}
	else if(type == 'album')
	{
		uri = uri.replace(/^https?:\/\/open\.spotify\.com\/album\/(.*?)$/, 'spotify:album:$1');
	}
	else if(type == 'track')
	{
		uri = uri.replace(/^https?:\/\/open\.spotify\.com\/track\/(.*?)$/, 'spotify:track:$1');
	}
	else if(type == 'local')
	{
		uri = uri.replace(/^https?:\/\/open\.spotify\.com\/local\/(.*?)$/, 'spotify:local:$1').replace(/\//g, ':');
	}

	return uri;
}

// Cookies

function isCookie(id)
{
	return (typeof $.cookie(id) != 'undefined');
}

function removeAllCookies()
{
	var cookies = $.cookie();

	for(var cookie in cookies)
	{
		if(cookies.hasOwnProperty(cookie)) $.removeCookie(cookie);
	}
}