package ru.kulikovd.gplus

import scala.concurrent.duration._

import akka.actor.{ActorRef, Actor}
import akka.pattern.ask
import akka.util.Timeout
import spray.http._
import spray.routing.HttpService
import scala.util.Success


class WidgetService(profileRepo: ActorRef) extends Actor with HttpService {
  import context.dispatcher

  implicit val timeout = Timeout(20 seconds)

  def actorRefFactory = context

  def receive = runRoute {
    get {
      (path("pingback") & parameters('profile, 'url)) { (profile, url) ⇒
        respondWithMediaType(MediaTypes.`application/javascript`) {
          complete {
            profileRepo ? ForwardTo(profile, GetActivityCommentsBy(url)) map {
              case CommentsFound(activ, comments) ⇒ render(activ, comments)
              case other ⇒ other.toString
            }
          }
        }
      }
    }
  }

  def render(post: Activity, comments: List[Comment]) = {

    val html = new StringBuilder

    html append s"""
      <style type="text/css">
          #gplus-pbwr * { margin: 0; padding: 0; border: none; line-height: 1.4; }
          #gplus-pbwr { padding: 20px 0; font-size: 13px; font-family: Arial, Tahoma, serif; }
          #gplus-pbwr .gplus-pbh { margin-bottom: 10px; border-top: #bbb 3px solid; padding: 10px 0 10px 22px; background: url("https://ssl.gstatic.com/s2/oz/images/favicon.ico") no-repeat left center; }
          #gplus-pbwr .gplus-pbh-title { font-size: 16px; font-weight: bold; text-decoration: none; }
          #gplus-pbwr .gplus-pbh-order { float: right; font-size: 12px; padding: 3px 0; }
          #gplus-pbwr .gplus-pbh-order a { text-decoration: none; margin-left: 3px; padding: 2px 4px; }
          #gplus-pbwr .gplus-pbh-order a u { text-decoration: none; border-bottom: 1px dotted; }
          #gplus-pbwr .gplus-pbh-order .gplus-active { background: #d0e7fd; color: black; }
          #gplus-pbwr .gplus-pbh-order .gplus-active u { border: none; }
          #gplus-pbwr .gplus-pbi { border-top: #ccc 1px dotted; position: relative; padding: 8px 8px 8px 50px; }
          #gplus-pbwr .gplus-pbi-avatar { position: absolute; top: 8px; left: 8px; text-decoration: none; display: block; }
          #gplus-pbwr .gplus-pbi-avatar img { width: 32px; height: 32px; }
          #gplus-pbwr .gplus-pbi-author { text-decoration: none; }
          #gplus-pbwr .gplus-pbi-text { padding-bottom: 2px; }
          #gplus-pbwr .gplus-pbi-date { margin-right: 10px; color: #999; }
          #gplus-pbwr .gplus-pbi-plusone { color: #3366CC; font-style: italic; font-weight: bold; }
          #gplus-pbwr .glpus-pb-footer { background: url("http://gplus.kulikovd.ru/google-plus-16x16.png") no-repeat scroll 24px 7px transparent; padding: 5px 50px; }
          #gplus-pbwr .glpus-pb-footer a { font-size: 14px; text-decoration: underline; }
      </style>
      <div id="gplus-pbwr">
      <div class="gplus-pbh">
          ${if (comments.size > 2) { 
            <div class="gplus-pbh-order">
              <a href="#" onclick="GplusApi.sortBy(this, \'date\', 1); return false;"><u>новые снизу</u> &darr;</a>
              <a href="#" onclick="GplusApi.sortBy(this, \'date\', 0); return false;"><u>новые сверху</u> &uarr;</a>
              <a href="#" onclick="GplusApi.sortBy(this, \'gplus\'); return false;"><u>по рейтингу</u> +1</a>
            </div>
          }}
          <a class="gplus-pbh-title" href="${post.url}" target="_blank">
            ${comments.size} комментариев из Google+
          </a>
      </div>
      <div id="gplus-pbwr-items">
      """

    if (comments.nonEmpty) {
      comments foreach { comment =>
        val pgVal = comment.plusOneValue
        html append s"""<div class="gplus-pbi" date="${comment.date}" gplus="$pgVal">
          <a href="${comment.authorProfileUrl}" class="gplus-pbi-avatar">
            <img src="${comment.authorImageUrl}&sz=32" />
          </a>
          <div class="gplus-pbi-text">
            <a href="${comment.authorProfileUrl}" class="gplus-pbi-author" target="_blank">${comment.authorName}</a>&nbsp;&ndash;
            ${comment.text}
          </div>
          <div>
            <span class="gplus-pbi-date">${comment.date}</span>
            ${if (pgVal > 0) <span class="gplus-pbi-plusone">pgVal</span> }
          </div>
        </div>"""
      }
      html append s"""<div class="glpus-pb-footer"><a href="${post.url}" target="_blank">Добавить комментарий в Google+</a></div>"""
    } else {
      html append s"""<div style="margin: -15px 22px 30px;">Пока ничего нет. <a href="${post.url}" target="_blank">Добавить комментарий...</a></div>"""
    }

    html append "</div>"

    s"""(function() {
        var _g = document.getElementById('gplus-pingback');
        if (!_g) {
            _g = document.createElement('div'); _g.id = 'gplus-pingback';
            var _c = document.getElementById('comments') || document.getElementById('disqus_thread'); 
            if (!_c) return;
            _c.parentNode.insertBefore(_g, _c.nextSibling);
        }
        _g.innerHTML = '${html.toString.replaceAll("'", "\\\'").replaceAll("\n", "\\\n").replaceAll("\r", "\\\r")}';

        GplusApi = {
            sortBy: function(button, field, dir) {
                var list = document.getElementById('gplus-pbwr-items'), items = list.childNodes, itemsArr = [];
                for (var i in items) items[i].nodeType == 1 && itemsArr.push(items[i]);
                itemsArr.sort(function(a, b) {
                    a = parseInt(a.getAttribute(field), 10), b = parseInt(b.getAttribute(field), 10);
                    return a == b ? 0 : (a > b ? (dir ? 1 : -1) : (dir ? -1 : 1));
                });
                for (var i = 0, _cnt = itemsArr.length; i < _cnt; i++) {
                  list.appendChild(itemsArr[i]);
                }
                var bns = button.parentNode.getElementsByTagName('a');
                for (i in bns) bns[i].className = '';
                button.className = 'gplus-active';
            }
        };
    })();""".replaceAll("\\s+", " ")
  }
}
