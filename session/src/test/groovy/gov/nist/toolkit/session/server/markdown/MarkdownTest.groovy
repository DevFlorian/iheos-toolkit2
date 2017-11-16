package gov.nist.toolkit.session.server.markdown

import spock.lang.Specification

/**
 *
 */
class MarkdownTest extends Specification {

    def 'bold' () {
        when:
        def markdown = 'This is my **bold** text.'
        def html = '''<p>This is my <span style="font-weight:bold">bold</span> text.'''

        then:
        Markdown.toHtml(markdown).trim() == html
    }

    def 'leading bold' () {
        when:
        def markdown = '**This** is my bold text.'
        def html = '''<p><span style="font-weight:bold">This</span> is my bold text.'''

        then:
        Markdown.toHtml(markdown).trim() == html
    }

    def 'one para'() {
        when:
        def markdown = 'This is a test'
        def html = '''<p>This is a test'''.trim()

        then:
        Markdown.toHtml(markdown).trim() == html
    }

    def 'two para'() {
        when:
        def markdown = '''
This is a test

para 2
'''.trim()
        def html = '''<p>This is a test <p>para 2'''.trim()

        then:
        Markdown.toHtml(markdown).trim() == html
    }

    def 'two multiline para'() {
        when:
        def markdown = '''
This is a test.
It really is.

para 2
Or so I say it is.
'''.trim()
        def html = '''
<p>
This is a test. It really is. <p>
para 2 Or so I say it is.
'''.trim().replace('\n','')

        then:
        Markdown.toHtml(markdown).trim() == html
    }

    def 'heading 1'() {
        when:
        def markdown = '# My Test'
        def html = "<h1>My Test</h1>"

        then:
        Markdown.toHtml(markdown).trim() == html
    }

    def 'heading 2'() {
        when:
        def markdown = '## My Test'
        def html = "<h2>My Test</h2>"

        then:
        Markdown.toHtml(markdown).trim() == html
    }

    def 'heading 3'() {
        when:
        def markdown = '### My Test'
        def html = "<h3>My Test</h3>"

        then:
        Markdown.toHtml(markdown).trim() == html
    }

    def 'heading 4'() {
        when:
        def markdown = '#### My Test'
        def html = "<h4>My Test</h4>"

        then:
        Markdown.toHtml(markdown).trim() == html
    }

    def 'two paragraph'() {
        when:
        def markdown = '''
This is a test.

Had this been a real emergency.
'''.trim()
        def html = '''
<p>
This is a test. <p>
Had this been a real emergency.
'''.trim().replace('\n','')

        then:
        Markdown.toHtml(markdown).trim() == html
    }

    def 'unordered list'() {
        when:
        def markdown = '''
* One
* Two
* Three
'''.trim()
        def html = '''
<ul>
<li>One</li>
<li>Two</li>
<li>Three</li>
</ul>
'''.trim().replace('\n', '')

        then:
        Markdown.toHtml(markdown).trim() == html
    }

    def 'unordered list in context'() {
        when:
        def markdown = '''
First paragraph.

* One
* Two
* Three

Last paragraph.
'''.trim()
        def html = '''
<p>
First paragraph. <ul>
<li>One</li>
<li>Two</li>
<li>Three</li>
</ul> <p>
Last paragraph.
'''.trim().replace('\n', '')

        then:
        Markdown.toHtml(markdown).trim() == html
    }

    def 'ordered list'() {
        when:
        def markdown = '''
1. One
2. Two
3. Three
'''
        def html = '''
<ol>
<li>One</li>
<li>Two</li>
<li>Three</li>
</ol>
'''.trim().replace('\n', '')

        then:
        Markdown.toHtml(markdown).trim() == html
    }

    def 'ordered list in context'() {
        when:
        def markdown = '''
First paragraph.

1. One
2. Two
3. Three

Last paragraph.
'''
        def html = '''
<p>
First paragraph. <ol>
<li>One</li>
<li>Two</li>
<li>Three</li>
</ol> <p>
Last paragraph.
'''.trim().replace('\n', '')

        then:
        Markdown.toHtml(markdown).trim() == html
    }

    def 'combo'() {
        when:
        def markdown = '''
# This is my test

About my test.

## Section one

1. Step 1
2. Step 2

## Section two

There is no section two.
'''
        def html= '''
<h1>This is my test</h1> <p>
About my test. <h2>Section one</h2> <ol>
<li>Step 1</li>
<li>Step 2</li>
</ol> <h2>Section two</h2> <p>
There is no section two.
'''.trim().replace('\n', '')

        then:
        Markdown.toHtml(markdown).trim() == html

    }

    def 'empty lines' () {
        when:
        def markdown = '''
Line 1.


Line 2.
'''.trim()
        def html = '''
<p>
Line 1. <p>
Line 2.
'''.trim().replace('\n', '')

        then:
        Markdown.toHtml(markdown).trim() == html
    }

//    def 'para 1'() {
//        when:
//        def markdown = '''
//            Lookup the Patient resource submitted in section patient1 of test
//            supporting_fhir_patients which is created by a conformance test for the actor
//            FHIR Support. This fake actor exists only to build a supporting FHIR environment
//            to support tests like this one.
//'''
//        def html= '''
//'''.trim().replace('\n', '')
//
//        then:
//        Markdown.toHtml(markdown).trim() == html
//
//    }
}
