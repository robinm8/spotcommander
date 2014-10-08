<?php

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

require_once('main.php');

$nowplaying = get_nowplaying();

$playbackstatus = $nowplaying['playbackstatus'];

$play_pause = 'play';
$volume = 50;
$artist = 'Unknown';
$title = 'Spotify is Not Running';
$album = 'Unknown';
$cover_art = 'img/no-cover-art-640.png?' . project_serial;
$uri = '';
$is_local = false;
$length = 'Unknown';
$released = 'Unknown';
$popularity = 'Unknown';

$actions = array();
$actions[] = array('action' => array('Recently Played', ''), 'keys' => array('actions', 'activity', 'subactivity', 'args'), 'values' => array('change_activity', 'recently-played', '', ''));
$actions[] = array('action' => array('Suspend Computer', ''), 'keys' => array('actions'), 'values' => array('confirm_suspend_computer'));
$actions[] = array('action' => array('Shut Down Computer', ''), 'keys' => array('actions'), 'values' => array('confirm_shut_down_computer'));

if(spotify_is_running())
{
	if($playbackstatus == 'Playing' || $playbackstatus == 'Paused')
	{
		$is_local = (get_uri_type($nowplaying['url']) == 'local');

		$play_pause = ($playbackstatus == 'Playing') ? 'pause' : 'play';
		$volume = get_current_volume();
		$artist = (empty($nowplaying['artist'])) ? $artist : $nowplaying['artist'];
		$title = (empty($nowplaying['title'])) ? $title : $nowplaying['title'];
		$album = (empty($nowplaying['album'])) ? $album : $nowplaying['album'];
		$uri = ($is_local) ? preg_replace('/:\d*$/', '', $nowplaying['url']) . ':' : $nowplaying['url'];
		$length = convert_length($nowplaying['length'], 'mc');

		if(!empty($nowplaying['artUrl']))
		{
			$cover_art = $nowplaying['artUrl'];
			$cover_art = str_replace(array('open.spotify.com', '/thumb/'), array('o.scdn.co', '/640/'), $cover_art);
			$cover_art = (get_uri_type($cover_art) == 'cover_art') ? $cover_art : 'img/no-cover-art-640.png?' . project_serial;
		}

		if(!empty($nowplaying['contentCreated']))
		{
			$released = $nowplaying['contentCreated'];
			$released = substr($released, 0, 4);
		}

		if(!empty($nowplaying['autoRating']))
		{
			$popularity = $nowplaying['autoRating'];
			$popularity = convert_popularity($popularity);
		}

		if($playbackstatus == 'Playing') save_recently_played($artist, $title, $uri);

		$details_dialog = array();
		$details_dialog['title'] = hsc($title);
		$details_dialog['details'][] = array('detail' => 'Album', 'value' => $album);
		$details_dialog['details'][] = array('detail' => 'Released', 'value' => $released);
		$details_dialog['details'][] = array('detail' => 'Length', 'value' => $length);
		$details_dialog['details'][] = array('detail' => 'Popularity', 'value' => $popularity);

		$actions_dialog = array();
		$actions_dialog['title'] = hsc($title);
		$actions_dialog['actions'][] = array('text' => 'Add to Playlist', 'keys' => array('actions', 'title', 'uri', 'isauthorizedwithspotify'), 'values' => array('hide_dialog add_to_playlist', $title, $uri, is_authorized_with_spotify));
		$actions_dialog['actions'][] = array('text' => 'Go to Artist', 'keys' => array('actions', 'uri'), 'values' => array('hide_dialog browse_artist', $uri));
		$actions_dialog['actions'][] = array('text' => 'Search Artist', 'keys' => array('actions', 'string'), 'values' => array('hide_dialog get_search', rawurlencode('artist:"' . $artist . '"')));
		$actions_dialog['actions'][] = array('text' => 'Start Track Radio', 'keys' => array('actions', 'uri', 'playfirst'), 'values' => array('hide_dialog start_track_radio', $uri, 'false'));
		$actions_dialog['actions'][] = array('text' => 'Share', 'keys' => array('actions', 'title', 'uri'), 'values' => array('hide_dialog share_uri', hsc($title), rawurlencode(uri_to_url($uri))));
		$actions_dialog['actions'][] = array('text' => 'YouTube', 'keys' => array('actions', 'uri'), 'values' => array('open_external_activity', 'https://www.youtube.com/results?search_query=' . rawurlencode($artist . ' ' . $title)));
		$actions_dialog['actions'][] = array('text' => 'Last.fm', 'keys' => array('actions', 'uri'), 'values' => array('open_external_activity', 'http://www.last.fm/music/' . urlencode($artist) . '/_/' . urlencode($title)));
		$actions_dialog['actions'][] = array('text' => 'Wikipedia', 'keys' => array('actions', 'uri'), 'values' => array('open_external_activity', 'https://en.wikipedia.org/wiki/Special:Search?search=' . rawurlencode($artist)));
		$actions_dialog['actions'][] = array('text' => 'Pause After Track', 'keys' => array('actions', 'queueaction', 'sortorder'), 'values' => array('hide_dialog queue_action', 'pause', 'top'));
		$actions_dialog['actions'][] = array('text' => 'Stop After track', 'keys' => array('actions', 'queueaction', 'sortorder'), 'values' => array('hide_dialog queue_action', 'stop', 'top'));
		$actions_dialog['actions'][] = array('text' => 'Suspend After Track', 'keys' => array('actions', 'queueaction', 'sortorder'), 'values' => array('hide_dialog queue_action', 'suspend', 'top'));
		$actions_dialog['actions'][] = array('text' => 'Shut Down After Track', 'keys' => array('actions', 'queueaction', 'sortorder'), 'values' => array('hide_dialog queue_action', 'shutdown', 'top'));
		$actions_dialog['actions'][] = array('text' => 'Suspend Computer', 'keys' => array('actions'), 'values' => array('hide_dialog confirm_suspend_computer'));
		$actions_dialog['actions'][] = array('text' => 'Shut Down Computer', 'keys' => array('actions'), 'values' => array('hide_dialog confirm_shut_down_computer'));

		$library_action = (is_saved($uri) == 'save') ? 'Save to Library' : 'Remove from Library';

		$actions = array();
		$actions[] = array('action' => array('Recently Played', ''), 'keys' => array('actions', 'activity', 'subactivity', 'args'), 'values' => array('change_activity', 'recently-played', '', ''));
		$actions[] = array('action' => array('Show Queue', ''), 'keys' => array('actions', 'activity', 'subactivity', 'args'), 'values' => array('change_activity', 'queue', '', ''));

		if(daemon_pulseaudio_check()) $actions[] = (get_volume_control() == 'spotify') ? array('action' => array('System Volume', ''), 'keys' => array('actions', 'volumecontrol'), 'values' => array('adjust_volume_control', 'system')) : array('action' => array('Spotify\'s Volume', ''), 'keys' => array('actions', 'volumecontrol'), 'values' => array('adjust_volume_control', 'spotify'));

		$actions[] = array('action' => array($library_action, ''), 'keys' => array('actions', 'artist', 'title', 'uri', 'isauthorizedwithspotify'), 'values' => array('save', rawurlencode($artist), rawurlencode($title), $uri, is_authorized_with_spotify));
		$actions[] = array('action' => array('Lyrics', ''), 'keys' => array('actions', 'activity', 'subactivity', 'args'), 'values' => array('change_activity', 'lyrics', '', 'artist=' . rawurlencode($artist) . '&amp;title=' . rawurlencode($title)));
		$actions[] = array('action' => array('Queue', ''), 'keys' => array('actions', 'artist', 'title', 'uri'), 'values' => array('queue_uri', rawurlencode($artist), rawurlencode($title), $uri));
		$actions[] = array('action' => array('Details', ''), 'keys' => array('actions', 'dialogdetails'), 'values' => array('show_details_dialog', base64_encode(json_encode($details_dialog))));
		$actions[] = array('action' => array('More...', ''), 'keys' => array('actions', 'dialogactions'), 'values' => array('show_actions_dialog', base64_encode(json_encode($actions_dialog))));
	}
	else
	{
		$title = 'No Music is Playing';
	}
}

$metadata = array();
$metadata['project_version'] = project_version;
$metadata['play_pause'] = $play_pause;
$metadata['artist'] = $artist;
$metadata['title'] = $title;
$metadata['album'] = $album;
$metadata['cover_art'] = $cover_art;
$metadata['uri'] = $uri;
$metadata['is_local'] = $is_local;
$metadata['tracklength'] = $length;
$metadata['released'] = $released;
$metadata['current_volume'] = $volume;
$metadata['actions'] = $actions;

echo json_encode($metadata);

?>
