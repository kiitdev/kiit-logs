/**
 *  <kiit_header>
 * url: www.kiit.dev
 * git: www.github.com/slatekit/kiit
 * org: www.codehelix.co
 * author: Kishore Reddy
 * copyright: 2016 CodeHelix Solutions Inc.
 * license: refer to website and/or github
 *
 *
 *  </kiit_header>
 */

package kiit.logs

/**
 * Where an entry comes from: the system that owns it and where inside that system. Written `origin:scope`, the same
 * form as a service id in kiit-service-id, e.g. `shop.example.com:orders.payment`.
 *
 * @param origin who owns the system that emits the logs, set once for the app, e.g. "shop.example.com". A domain or
 *               any other stable id. Same convention as origin in kiit-codes and kiit-service-id
 * @param scope free-form label for where in the origin this is, e.g. "orders.checkout". Dots express hierarchy.
 *              Same convention as scope in kiit-codes and kiit-service-id. It should not contain a colon, which
 *              separates it from the origin in [text]. This is not checked. Empty means no scope
 */
data class Source(val origin: String, val scope: String = "") {
    /**
     * `origin:scope`. The colon stays when there is no scope, so a missing scope is visible: `shop.example.com:`.
     */
    val text: String get() = "$origin:$scope"

    companion object {
        /**
         * The source used when none is given, e.g. by [LogSettings.safe], so safe settings need no arguments.
         */
        val DEFAULT = Source("app")
    }
}
