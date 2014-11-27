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

$activity = array();
$activity['project_version'] = project_version;

if(isset($_GET['featured-playlists']))
{
	$files = get_external_files(array(project_website . 'api/1/browse/featured-playlists/?time=' . $_GET['time'] . '&country=' . $_GET['country'] . '&fields=' . rawurlencode('description,playlists')), null, null);
	$metadata = json_decode($files[0], true);

	if(empty($metadata['metadata']))
	{
		$activity['title'] = 'Error';
		$activity['actions'][] = array('action' => array('Retry', 'refresh_white_24_img_div'), 'keys' => array('actions'), 'values' => array('reload_activity'));

		echo '
			<div id="activity_inner_div" data-activitydata="' . base64_encode(json_encode($activity)) . '">

			<div id="activity_message_div"><div><div class="img_div img_48_div information_grey_48_img_div"></div></div><div>Could not get playlists. Try again.</div></div>

			</div>
		';
	}
	else
	{
		$metadata = $metadata['metadata'];
		$playlists = $metadata['playlists'];

		$activity['title'] = hsc($metadata['description']);

		echo '
			<div id="activity_inner_div" data-activitydata="' . base64_encode(json_encode($activity)) . '">

			<div class="cards_vertical_div">
		';

		foreach($playlists as $playlist)
		{
			$name = $playlist['name'];
			$description = $playlist['description'];
			$followers = $playlist['followers_formatted'];
			$uri = $playlist['uri'];
			$cover_art = $playlist['cover_art'];

			echo '<div class="card_vertical_div"><div title="' . hsc($description) . '" class="card_vertical_inner_div actions_div" data-actions="browse_playlist" data-uri="' . $uri . '" data-description="' . rawurlencode($description) . '" data-isauthorizedwithspotify="' . boolean_to_string(is_authorized_with_spotify) . '" data-highlightclass="card_highlight" onclick="void(0)"><div class="card_vertical_cover_art_div" style="background-image: url(\'' . $cover_art . '\')"></div><div class="card_vertical_upper_div">' . hsc($name) . '</div><div class="card_vertical_lower_div">Followers: ' . $followers . '</div></div></div>';
		}

		echo '<div class="clear_float_div"></div></div></div>';
	}
}
elseif(isset($_GET['top-lists']))
{
	$country = get_spotify_country();

	$activity['title'] = 'Top Lists in ' . get_country_name($country);

	$files = get_external_files(array(project_website . 'api/1/browse/top-lists/?country=' . $country), null, null);
	$playlists = json_decode($files[0], true);

	if(!is_array($playlists))
	{
		$activity['actions'][] = array('action' => array('Retry', 'refresh_white_24_img_div'), 'keys' => array('actions'), 'values' => array('reload_activity'));

		echo '
			<div id="activity_inner_div" data-activitydata="' . base64_encode(json_encode($activity)) . '">

			<div id="activity_message_div"><div><div class="img_div img_48_div information_grey_48_img_div"></div></div><div>Could not get playlists. Try again.</div></div>

			</div>
		';
	}
	else
	{
		echo '
			<div id="activity_inner_div" data-activitydata="' . base64_encode(json_encode($activity)) . '">

			<div class="cards_vertical_div">
		';

		foreach($playlists as $playlist)
		{
			$name = $playlist['name'];
			$description = $playlist['description'];
			$followers = $playlist['followers_formatted'];
			$uri = $playlist['uri'];
			$cover_art = $playlist['cover_art'];

			echo '<div class="card_vertical_div"><div title="' . hsc($description) . '" class="card_vertical_inner_div actions_div" data-actions="browse_playlist" data-uri="' . $uri . '" data-description="' . rawurlencode($description) . '" data-isauthorizedwithspotify="' . boolean_to_string(is_authorized_with_spotify) . '" data-highlightclass="card_highlight" onclick="void(0)"><div class="card_vertical_cover_art_div" style="background-image: url(\'' . $cover_art . '\')"></div><div class="card_vertical_upper_div">' . hsc($name) . '</div><div class="card_vertical_lower_div">Followers: ' . $followers . '</div></div></div>';
		}

		echo '<div class="clear_float_div"></div></div></div>';
	}
}
elseif(isset($_GET['popular-playlists']))
{
	$activity['title'] = 'Popular Playlists';

	$files = get_external_files(array(project_website . 'api/2/browse/popular-playlists/'), null, null);
	$playlists = json_decode($files[0], true);

	if(!is_array($playlists))
	{
		$activity['actions'][] = array('action' => array('Retry', 'refresh_white_24_img_div'), 'keys' => array('actions'), 'values' => array('reload_activity'));

		echo '
			<div id="activity_inner_div" data-activitydata="' . base64_encode(json_encode($activity)) . '">

			<div id="activity_message_div"><div><div class="img_div img_48_div information_grey_48_img_div"></div></div><div>Could not get playlists. Try again.</div></div>

			</div>
		';
	}
	else
	{
		echo '
			<div id="activity_inner_div" data-activitydata="' . base64_encode(json_encode($activity)) . '">

			<div class="cards_vertical_div">
		';

		foreach($playlists as $playlist)
		{
			$name = $playlist['name'];
			$description = $playlist['description'];
			$genre = $playlist['genre'];
			$uri = $playlist['uri'];
			$cover_art = $playlist['cover_art'];

			echo '<div class="card_vertical_div"><div title="' . hsc($description) . '" class="card_vertical_inner_div actions_div" data-actions="browse_playlist" data-uri="' . $uri . '" data-description="' . rawurlencode($description) . '" data-isauthorizedwithspotify="' . is_authorized_with_spotify . '" data-highlightclass="card_highlight" onclick="void(0)"><div class="card_vertical_cover_art_div" style="background-image: url(\'' . $cover_art . '\')"></div><div class="card_vertical_upper_div">' . hsc($name) . '</div><div class="card_vertical_lower_div">' . hsc($genre) . '</div></div></div>';
		}

		echo '<div class="clear_float_div"></div></div></div>';
	}
}
elseif(isset($_GET['genres']))
{
	$country = get_spotify_country();

	if(isset($_GET['name']) && isset($_GET['genre']))
	{
		$activity['title'] = rawurldecode($_GET['name']);

		$files = get_external_files(array(project_website . 'api/1/browse/genre/?genre=' . $_GET['genre'] . '&country=' . $country), null, null);
		$playlists = json_decode($files[0], true);

		if(!is_array($playlists))
		{
			$activity['actions'][] = array('action' => array('Retry', 'refresh_white_24_img_div'), 'keys' => array('actions'), 'values' => array('reload_activity'));

			echo '
				<div id="activity_inner_div" data-activitydata="' . base64_encode(json_encode($activity)) . '">

				<div id="activity_message_div"><div><div class="img_div img_48_div information_grey_48_img_div"></div></div><div>Could not get genre. Try again.</div></div>

				</div>
			';
		}
		else
		{
			echo '
				<div id="activity_inner_div" data-activitydata="' . base64_encode(json_encode($activity)) . '">

				<div class="cards_vertical_div">
			';

			foreach($playlists as $playlist)
			{
				$name = $playlist['name'];
				$description = $playlist['description'];
				$followers = $playlist['followers_formatted'];
				$uri = $playlist['uri'];
				$cover_art = $playlist['cover_art'];

				echo '<div class="card_vertical_div"><div title="' . hsc($name . ': ' . $description) . '" class="card_vertical_inner_div actions_div" data-actions="browse_playlist" data-uri="' . $uri . '" data-description="' . rawurlencode($description) . '" data-isauthorizedwithspotify="' . boolean_to_string(is_authorized_with_spotify) . '" data-highlightclass="card_highlight" onclick="void(0)"><div class="card_vertical_cover_art_div" style="background-image: url(\'' . $cover_art . '\')"></div><div class="card_vertical_upper_div">' . hsc($name) . '</div><div class="card_vertical_lower_div">Followers: ' . $followers . '</div></div></div>';
			}

			echo '<div class="clear_float_div"></div></div></div>';
		}
	}
	else
	{
		$activity['title'] = 'Genres &amp; Moods';

		$files = get_external_files(array(project_website . 'api/1/browse/genres/?country=' . $country), null, null);
		$genres = json_decode($files[0], true);

		if(!is_array($genres))
		{
			$activity['actions'][] = array('action' => array('Retry', 'refresh_white_24_img_div'), 'keys' => array('actions'), 'values' => array('reload_activity'));

			echo '
				<div id="activity_inner_div" data-activitydata="' . base64_encode(json_encode($activity)) . '">

				<div id="activity_message_div"><div><div class="img_div img_48_div information_grey_48_img_div"></div></div><div>Could not get genres. Try again.</div></div>

				</div>
			';
		}
		else
		{
			echo '
				<div id="activity_inner_div" data-activitydata="' . base64_encode(json_encode($activity)) . '">

				<div class="cards_vertical_div">
			';

			foreach($genres as $genre)
			{
				$name = $genre['name'];
				$space = $genre['space'];
				$cover_art = $genre['cover_art'];

				echo '<div class="card_vertical_div"><div title="' . hsc($name) . '" class="card_vertical_inner_div actions_div" data-actions="change_activity" data-activity="browse" data-subactivity="genres" data-args="name=' . rawurlencode($name) . '&amp;genre=' . $space . '" data-highlightclass="card_highlight" onclick="void(0)"><div class="card_vertical_cover_art_div" style="background-image: url(\'' . $cover_art . '\')"></div><div class="card_vertical_upper_div">' . hsc($name) . '</div><div class="card_vertical_lower_div">Genre</div></div></div>';
			}

			echo '<div class="clear_float_div"></div></div></div>';
		}
	}
}
elseif(isset($_GET['new-releases']))
{
	$country = get_spotify_country();

	$activity['title'] = 'New Releases in ' . get_country_name($country);

	$files = get_external_files(array(project_website . 'api/1/browse/new-releases/?country=' . $country), null, null);
	$albums = json_decode($files[0], true);

	if(!is_array($albums))
	{
		$activity['actions'][] = array('action' => array('Retry', 'refresh_white_24_img_div'), 'keys' => array('actions'), 'values' => array('reload_activity'));

		echo '
			<div id="activity_inner_div" data-activitydata="' . base64_encode(json_encode($activity)) . '">

			<div id="activity_message_div"><div><div class="img_div img_48_div information_grey_48_img_div"></div></div><div>Could not get releases. Try again.</div></div>

			</div>
		';
	}
	else
	{
		echo '
			<div id="activity_inner_div" data-activitydata="' . base64_encode(json_encode($activity)) . '">

			<div class="cards_vertical_div">
		';

		foreach($albums as $album)
		{
			$artist = $album['artist'];
			$title = $album['title'];
			$released = $album['released'];
			$uri = $album['uri'];
			$cover_art = $album['cover_art'];

			echo '<div class="card_vertical_div"><div title="' . hsc($artist . ' - ' . $title) . ' (' . $released . ')" class="card_vertical_inner_div actions_div" data-actions="browse_album" data-uri="' . $uri . '" data-highlightclass="card_highlight" onclick="void(0)"><div class="card_vertical_cover_art_div" style="background-image: url(\'' . $cover_art . '\')"></div><div class="card_vertical_upper_div">' . hsc($title) . '</div><div class="card_vertical_lower_div">' . hsc($artist) . '</div></div></div>';
		}

		echo '<div class="clear_float_div"></div></div></div>';
	}
}
elseif(isset($_GET['charts']))
{
	$country = get_spotify_country();

	$chart = $_GET['chart'];

	$files = get_external_files(array(project_website . 'api/1/browse/charts/?chart=' . $chart . '&country=' . $country), null, null);
	$metadata = json_decode($files[0], true);

	$activity['title'] = 'Most ' . ucfirst($chart) . ' in ' . get_country_name($country);

	if(empty($metadata['tracks']))
	{
		$activity['actions'][] = array('action' => array('Retry', 'refresh_white_24_img_div'), 'keys' => array('actions'), 'values' => array('reload_activity'));

		echo '
			<div id="activity_inner_div" data-activitydata="' . base64_encode(json_encode($activity)) . '">

			<div id="activity_message_div"><div><div class="img_div img_48_div information_grey_48_img_div"></div></div><div>Could not get chart. Try again.</div></div>

			</div>
		';
	}
	else
	{
		$tracks = $metadata['tracks'];

		$add_to_playlist_uris = '';
		$queue_uris = array();
		$i = 0;

		foreach($tracks as $track)
		{
			$track_uri = url_to_uri($track['uri']);

			$add_to_playlist_uris .= $track_uri . ' ';

			$queue_uris[$i]['artist'] = $track['artist'];
			$queue_uris[$i]['title'] = $track['title'];
			$queue_uris[$i]['uri'] = $track_uri;

			$i++;
		}

		$add_to_playlist_uris = trim($add_to_playlist_uris);
		$queue_uris = base64_encode(json_encode($queue_uris));

		$activity['actions'][] = array('action' => array('Add to Playlist', ''), 'keys' => array('actions', 'title', 'uri', 'isauthorizedwithspotify'), 'values' => array('hide_dialog add_to_playlist', $activity['title'], $add_to_playlist_uris, is_authorized_with_spotify));
		$activity['actions'][] = array('action' => array('Queue Tracks', ''), 'keys' => array('actions', 'uris', 'randomly'), 'values' => array('hide_dialog queue_uris', $queue_uris, 'false'));
		$activity['fab'] = array('label' => 'Queue Tracks Randomly', 'icon' => 'queue_white_24_img_div', 'keys' => array('actions', 'uris', 'randomly'), 'values' => array('queue_uris', $queue_uris, 'true'));

		echo '
			<div id="activity_inner_div" data-activitydata="' . base64_encode(json_encode($activity)) . '">

			<div class="list_header_div"><div>Tracks</div><div></div></div>

			<div class="list_div">
		';

		foreach($tracks as $track)
		{
			$artist = $track['artist'];
			$title = $track['title'];
			$uri = url_to_uri($track['uri']);
			$plays = $track['plays_formatted'];

			$details_dialog = array();
			$details_dialog['title'] = hsc($title);
			$details_dialog['details'][] = array('detail' => 'Plays', 'value' => $plays);

			$actions_dialog = array();
			$actions_dialog['title'] = hsc($title);
			$actions_dialog['actions'][] = array('text' => 'Add to Playlist', 'keys' => array('actions', 'title', 'uri', 'isauthorizedwithspotify'), 'values' => array('hide_dialog add_to_playlist', $title, $uri, is_authorized_with_spotify));
			$actions_dialog['actions'][] = array('text' => 'Go to Album', 'keys' => array('actions', 'uri'), 'values' => array('hide_dialog browse_album', $uri));
			$actions_dialog['actions'][] = array('text' => 'Search Artist', 'keys' => array('actions', 'string'), 'values' => array('hide_dialog get_search', rawurlencode('artist:"' . $artist . '"')));
			$actions_dialog['actions'][] = array('text' => 'Start Track Radio', 'keys' => array('actions', 'uri', 'playfirst'), 'values' => array('hide_dialog start_track_radio', $uri, 'true'));
			$actions_dialog['actions'][] = array('text' => 'Lyrics', 'keys' => array('actions', 'artist', 'title'), 'values' => array('hide_dialog get_lyrics', rawurlencode($artist), rawurlencode($title)));
			$actions_dialog['actions'][] = array('text' => 'Details', 'keys' => array('actions', 'dialogdetails'), 'values' => array('hide_dialog show_details_dialog', base64_encode(json_encode($details_dialog))));
			$actions_dialog['actions'][] = array('text' => 'Share', 'keys' => array('actions', 'title', 'uri'), 'values' => array('hide_dialog share_uri', hsc($title), rawurlencode(uri_to_url($uri))));

			echo '
				<div class="list_item_div">
				<div title="' . hsc($artist . ' - ' . $title) . '" class="list_item_main_div actions_div" data-actions="toggle_list_item_actions" data-trackuri="' . $uri . '" onclick="void(0)">
				<div class="list_item_main_actions_arrow_div"></div>
				<div class="list_item_main_inner_div">
				<div class="list_item_main_inner_icon_div"><div class="img_div img_24_div unfold_more_grey_24_img_div ' . track_is_playing($uri, 'icon') . '"></div></div>
				<div class="list_item_main_inner_text_div"><div class="list_item_main_inner_text_upper_div ' . track_is_playing($uri, 'text') . '">' . hsc($title) . '</div><div class="list_item_main_inner_text_lower_div">' . hsc($artist) . '</div></div>
				</div>
				</div>
				<div class="list_item_actions_div">
				<div class="list_item_actions_inner_div">
				<div title="Play" class="actions_div" data-actions="play_uri" data-uri="' . $uri . '" data-highlightclass="dark_grey_highlight" data-highlightotherelement="div.list_item_main_actions_arrow_div" data-highlightotherelementparent="div.list_item_div" data-highlightotherelementclass="up_arrow_dark_grey_highlight" onclick="void(0)"><div class="img_div img_24_div play_grey_24_img_div"></div></div>
				<div title="Queue" class="actions_div" data-actions="queue_uri" data-artist="' . rawurlencode($artist) . '" data-title="' . rawurlencode($title) . '" data-uri="' . $uri . '" data-highlightclass="dark_grey_highlight" onclick="void(0)"><div class="img_div img_24_div queue_grey_24_img_div"></div></div>
				<div title="Save to/Remove from Library" class="actions_div" data-actions="save" data-artist="' . rawurlencode($artist) . '" data-title="' . rawurlencode($title) . '" data-uri="' . $uri . '" data-isauthorizedwithspotify="' . boolean_to_string(is_authorized_with_spotify) . '" data-highlightclass="dark_grey_highlight" onclick="void(0)"><div class="img_div img_24_div ' . save_remove_icon($uri) . '_grey_24_img_div"></div></div>
				<div title="Go to Artist" class="actions_div" data-actions="browse_artist" data-uri="' . $uri . '" data-highlightclass="dark_grey_highlight" onclick="void(0)"><div class="img_div img_24_div person_grey_24_img_div"></div></div>
				<div title="More" class="show_actions_dialog_div actions_div" data-actions="show_actions_dialog" data-dialogactions="' . base64_encode(json_encode($actions_dialog)) . '" data-highlightclass="dark_grey_highlight" onclick="void(0)"><div class="img_div img_24_div more_grey_24_img_div"></div></div>
				</div>
				</div>
				</div>
			';
		}

		echo '</div></div>';
	}
}
elseif(isset($_GET['news']))
{
	$country = get_spotify_country();

	$append = (isset($_GET['id'])) ? '&id=' . $_GET['id'] : '';

	$files = get_external_files(array(project_website . 'api/1/browse/news/?country=' . $country . $append), null, null);
	$metadata = json_decode($files[0], true);

	$activity['title'] = 'News in ' . get_country_name($country);

	if(empty($metadata['news']))
	{
		$activity['actions'][] = array('action' => array('Retry', 'refresh_white_24_img_div'), 'keys' => array('actions'), 'values' => array('reload_activity'));

		echo '
			<div id="activity_inner_div" data-activitydata="' . base64_encode(json_encode($activity)) . '">

			<div id="activity_message_div"><div><div class="img_div img_48_div information_grey_48_img_div"></div></div><div>Could not get news. Try again.</div></div>

			</div>
		';
	}
	else
	{
		$articles = $metadata['news']['articles'];

		if(isset($_GET['id']))
		{
			$article = $articles[$_GET['id']];

			$date = $article['date'];
			$headline = $article['headline'];
			$leadin = $article['leadin'];
			$body = preg_replace(array('/<a.*?href="(.*?)".*?>(.*?)<\/a>/s', '/<blockquote.*?>(.*?)<\/blockquote>/s'), array('<span class="actions_span" data-actions="browse_uri" data-uri="$1" data-isauthorizedwithspotify="' . is_authorized_with_spotify . '" data-highlightclass="actions_span_highlight" onclick="void(0)">$2</span>', '<div class="browse_news_article_quote_div">$1</div>'), $article['body']);
			$cover_art = $article['cover_art_large'];

			$activity['title'] = hsc($headline);

			echo '<div id="activity_inner_div" data-activitydata="' . base64_encode(json_encode($activity)) . '">';

			echo '
				<div id="browse_news_article_div"><div id="browse_news_article_date_div">' . $date . '</div><div id="browse_news_article_headline_div">' . $headline . '</div><div id="browse_news_article_leadin_div">' . $leadin . '</div><img id="browse_news_article_cover_art_img" src="' . $cover_art . '" alt=""><div id="browse_news_article_body_div">' . $body . '</div></div>
			';

			echo '</div>';
		}
		else
		{
			echo '
				<div id="activity_inner_div" data-activitydata="' . base64_encode(json_encode($activity)) . '">

				<div class="cards_vertical_div">
			';

			foreach($articles as $id => $article)
			{
				$date = $article['date'];
				$headline = $article['headline'];
				$cover_art = $article['cover_art_small'];

				echo '<div class="card_vertical_div"><div title="' . hsc($headline) . '" class="card_vertical_inner_div actions_div" data-actions="change_activity" data-activity="browse" data-subactivity="news" data-args="id=' . $id . '" data-highlightclass="card_highlight" onclick="void(0)"><div class="card_vertical_cover_art_div" style="background-image: url(\'' . $cover_art . '\')"></div><div class="card_vertical_upper_tall_div">' . hsc($headline) . '</div><div class="card_vertical_lower_div">' . hsc($date) . '</div></div></div>';
			}

			echo '<div class="clear_float_div"></div></div></div>';
		}
	}
}
else
{
	$country = get_spotify_country();
	$country_code = $country;
	$country = get_country_name($country);

	$activity['title'] = 'Browse';

	echo '
		<div id="activity_inner_div" data-activitydata="' . base64_encode(json_encode($activity)) . '">

		<div id="browse_div">

		<div id="browse_featured_playlists_div" data-country="' . $country_code . '" data-highlightclass="card_highlight" onclick="void(0)"><div><div></div></div></div>

		<div class="cards_div">
		<div>
		<div>
		<div class="card_div actions_div" data-actions="change_activity" data-activity="browse" data-subactivity="top-lists" data-args="" data-highlightclass="card_highlight" onclick="void(0)"><div class="card_icon_div"><div class="img_div img_24_div star_grey_24_img_div"></div></div><div class="card_text_div"><div>Top Lists</div><div>In ' . $country . '.</div></div></div>
		<div class="card_div actions_div" data-actions="change_activity" data-activity="browse" data-subactivity="popular-playlists" data-args="" data-highlightclass="card_highlight" onclick="void(0)"><div class="card_icon_div"><div class="img_div img_24_div heart_grey_24_img_div"></div></div><div class="card_text_div"><div>Popular Playlists</div><div>Updated weekly.</div></div></div>
		<div class="card_div actions_div" data-actions="change_activity" data-activity="browse" data-subactivity="genres" data-args="" data-highlightclass="card_highlight" onclick="void(0)"><div class="card_icon_div"><div class="img_div img_24_div label_grey_24_img_div"></div></div><div class="card_text_div"><div>Genres &amp; Moods</div><div>Playlists based on genres and moods.</div></div></div>
		<div class="card_div actions_div" data-actions="change_activity" data-activity="browse" data-subactivity="new-releases" data-args="" data-highlightclass="card_highlight" onclick="void(0)"><div class="card_icon_div"><div class="img_div img_24_div album_grey_24_img_div"></div></div><div class="card_text_div"><div>New Releases</div><div>In ' . $country . '.</div></div></div>
		</div>
		<div>
		<div class="card_div actions_div" data-actions="change_activity" data-activity="browse" data-subactivity="charts" data-args="chart=streamed" data-highlightclass="card_highlight" onclick="void(0)"><div class="card_icon_div"><div class="img_div img_24_div headphones_grey_24_img_div"></div></div><div class="card_text_div"><div>Most Streamed</div><div>Last week in ' . $country . '.</div></div></div>
		<div class="card_div actions_div" data-actions="change_activity" data-activity="browse" data-subactivity="charts" data-args="chart=viral" data-highlightclass="card_highlight" onclick="void(0)"><div class="card_icon_div"><div class="img_div img_24_div share_grey_24_img_div"></div></div><div class="card_text_div"><div>Most Viral</div><div>Last week in ' . $country . '.</div></div></div>
		<div class="card_div actions_div" data-actions="change_activity" data-activity="browse" data-subactivity="news" data-args="" data-highlightclass="card_highlight" onclick="void(0)"><div class="card_icon_div"><div class="img_div img_24_div hot_grey_24_img_div"></div></div><div class="card_text_div"><div>News</div><div>Latest music news in ' . $country . '.</div></div></div>
		</div>
		</div>
		</div>

		</div>

		</div>
	';
}

?>
