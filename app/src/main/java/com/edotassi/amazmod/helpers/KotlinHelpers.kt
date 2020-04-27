package com.edotassi.amazmod.helpers

import org.tinylog.Logger


class KtLogger {
  companion object {
    fun debug(msg: String) {
      Logger.debug(msg as Any)
    }

    fun warn(msg: String) {
      Logger.warn(msg as Any)
    }

    fun warn(msg: String, vararg arguments: String?) {
      Logger.warn(msg, arguments)
    }

    fun trace(msg: String) {
      Logger.trace(msg as Any)
    }

    fun info(msg: String) {
      Logger.info(msg as Any)
    }

    fun error(msg: String) {
      Logger.error(msg as Any)
    }

    fun error(exception: Throwable?, message: String?, vararg arguments: String?) {
      Logger.error(exception, message, arguments)
    }
  }
}
