- 自律駆動型エージェントによるgroovyl-lsp拡張機能 開発構成
	- CIゲート
		- 静的解析ツール
			- error prone (構文チェク、リファクタ)
				- nullaway
			- spotless (フォーマッター)
			- archunit
		- ローカルゲート
			- lefthook
		- バイパス潰し
		  collapsed:: true
			-
			  ```
			  git config --local alias.push \
			  '!_git_block_nv_push() {
			      for a in "$@"; do
			          [[ $a == --no-verify ]] && {
			              echo "🚫  --no-verify は禁止です" >&2
			              exit 4
			          }
			      done
			      # 本物を再帰せずに呼ぶ
			      command git push "$@"
			  }; _git_block_nv_push'
			  
			  ```
			-
			  ```
			  git config --local alias.commit \
			  '!_git_block_nv_commit() {
			      for a in "$@"; do
			          case "$a" in
			              --no-verify|-n)
			                  echo "🚫  --no-verify (-n) は禁止されています" >&2
			                  exit 4
			                  ;;
			          esac
			      done
			      command git commit "$@"
			  }; _git_block_nv_commit'
			  
			  ```
		- coverage
			- jacoco→octocov
			- c8 (jsのカバレッジ計測)
		- :TODO の可視化
			- todo-to-issue
	- 開発フロー
		- TDD & BDD
			- あるべき振る舞いを先に定義する
			- 常にカバレッジを100%にキープする
		- アジャイル
			- 最小構成として常にE2Eが通る状態をキープする
		- ADR
	- 開発構成
		- LSPコア
			- java
				- di: Dagger
				- 関数型ライク vavr
					- 基本的にはtry-catchは使用しない
					- ステップベース
				- test: junit5
			- オニオンアーキテクチャ
		- VSCode extension
			- typescript
				- 現状特になし
	- 品質管理
		- test pyramidの徹底
			- テスト時間の肥大化は抑える必要があるため、実行時間の可視化は重要
			- テストの可視化ツール
			  https://github.com/radarsh/gradle-test-logger-plugin  
		- codeql